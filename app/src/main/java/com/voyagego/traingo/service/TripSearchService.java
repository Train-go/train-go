package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.CoachType;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.DayPrice;
import com.voyagego.traingo.model.dto.SeatAvailability;
import com.voyagego.traingo.model.request.TripSearchRequest;
import com.voyagego.traingo.model.response.DateStripItem;
import com.voyagego.traingo.model.response.TripCard;
import com.voyagego.traingo.model.response.TripSearchResult;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * Step 1 of a booking: the trips a customer can book between two stations on
 * one day (issue #26). A customer sees only SCHEDULED trips that have not
 * left yet (D8); filters and sorting run here so a copied link shows the same
 * list.
 */
@Service
@Transactional(readOnly = true)
public class TripSearchService {

	/** Length of the date strip, and how far its arrows move. */
	public static final int STRIP_DAYS = 7;
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	private static final NumberFormat MONEY = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));

	private static final String[] WEEKDAYS = { "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy",
			"Chủ nhật" };

	private final StationRepository stationRepository;
	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;

	public TripSearchService(StationRepository stationRepository, TripRepository tripRepository,
			TripSeatRepository tripSeatRepository) {
		this.stationRepository = stationRepository;
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
	}

	public Optional<Station> findStation(String code) {
		return code == null ? Optional.empty() : stationRepository.findByCode(code);
	}

	/**
	 * Bookable trips from {@code origin} to {@code destination} leaving on
	 * {@code request.getDate()}, filtered and sorted as the request asks. Free
	 * seats for the whole page come from one count query.
	 */
	public TripSearchResult search(TripSearchRequest request, Station origin, Station destination,
			LocalDateTime now) {
		LocalDateTime dayStart = request.getDate().atStartOfDay();
		List<Trip> trips = tripRepository.findBookable(origin.getId(), destination.getId(), dayStart,
				dayStart.plusDays(1), now);
		if (trips.isEmpty()) {
			return new TripSearchResult(List.of(), 0);
		}

		Map<Long, Map<CoachType, Long>> free = new HashMap<>();
		for (SeatAvailability row : tripSeatRepository
				.countAvailableByTripAndType(trips.stream().map(Trip::getId).toList())) {
			free.computeIfAbsent(row.getTripId(), id -> new HashMap<>()).put(row.getCoachType(), row.getAvailable());
		}

		List<TripCard> cards = trips.stream()
				.map(trip -> card(trip, free.getOrDefault(trip.getId(), Map.of())))
				.filter(inSlots(request))
				.filter(hasTypes(request))
				.filter(card -> !request.isHideSoldOut() || !card.soldOut())
				.sorted(order(request.sortKey()))
				.toList();
		return new TripSearchResult(cards, trips.size());
	}

	/**
	 * The seven days shown around {@code day}, each with the lowest price of its
	 * trips that still have a free seat. The strip moves in whole weeks counted
	 * from today, so picking a day inside it never shifts it, and it never shows
	 * a day before today. All seven days come from one query.
	 */
	public List<DateStripItem> dateStrip(Station origin, Station destination, LocalDate day, LocalDate today,
			LocalDateTime now) {
		long weeks = Math.max(0, ChronoUnit.DAYS.between(today, day)) / STRIP_DAYS;
		LocalDate start = today.plusDays(weeks * STRIP_DAYS);
		Map<LocalDate, DayPrice> prices = new HashMap<>();
		for (DayPrice price : tripRepository.findDayPrices(origin.getId(), destination.getId(), start.atStartOfDay(),
				start.plusDays(STRIP_DAYS).atStartOfDay(), now)) {
			prices.put(price.getDay(), price);
		}
		return start.datesUntil(start.plusDays(STRIP_DAYS)).map(date -> {
			DayPrice price = prices.get(date);
			boolean hasTrips = price != null;
			boolean soldOut = hasTrips && price.getMinPrice() == null;
			Long minPrice = hasTrips ? price.getMinPrice() : null;
			return new DateStripItem(date, dayLabel(date, today), minPrice, soldOut, hasTrips,
					weekday(date) + " " + date.format(DAY_MONTH) + (!hasTrips ? ", không có chuyến"
							: soldOut ? ", hết vé" : ", giá từ " + MONEY.format(minPrice) + "đ"));
		}).toList();
	}

	public static String dayLabel(LocalDate date, LocalDate today) {
		if (date.equals(today)) {
			return "Hôm nay";
		}
		if (date.equals(today.plusDays(1))) {
			return "Ngày mai";
		}
		return weekday(date);
	}

	public static String weekday(LocalDate date) {
		return WEEKDAYS[date.getDayOfWeek().getValue() - 1];
	}

	private static TripCard card(Trip trip, Map<CoachType, Long> free) {
		LocalDateTime departure = trip.getDepartureAt();
		LocalDateTime arrival = trip.getArrivalAt();
		int nextDay = (int) ChronoUnit.DAYS.between(departure.toLocalDate(), arrival.toLocalDate());
		return new TripCard(trip.getId(), trip.getTrain().getCode(), departure, arrival,
				durationLabel(Duration.between(departure, arrival)), nextDay, trip.getBasePrice(),
				free.getOrDefault(CoachType.SEAT, 0L), free.getOrDefault(CoachType.SLEEPER, 0L));
	}

	/** "1g45", or "8g" on the hour. A 29-hour run reads "29g05". */
	static String durationLabel(Duration duration) {
		long minutes = duration.toMinutes();
		long rest = minutes % 60;
		return (minutes / 60) + "g" + (rest == 0 ? "" : String.format("%02d", rest));
	}

	/**
	 * Morning 05:00-11:59, afternoon 12:00-17:59, evening 18:00-21:59, night
	 * 22:00-04:59 by departure time. No slot ticked keeps every trip.
	 */
	private static Predicate<TripCard> inSlots(TripSearchRequest request) {
		return card -> request.getSlot().isEmpty() || request.getSlot().contains(slotOf(card.departureAt()));
	}

	private static String slotOf(LocalDateTime departure) {
		int hour = departure.getHour();
		if (hour >= 5 && hour < 12) {
			return TripSearchRequest.SLOT_MORNING;
		}
		if (hour >= 12 && hour < 18) {
			return TripSearchRequest.SLOT_AFTERNOON;
		}
		if (hour >= 18 && hour < 22) {
			return TripSearchRequest.SLOT_EVENING;
		}
		return TripSearchRequest.SLOT_NIGHT;
	}

	/** A trip stays when it has at least one free seat of a ticked type. */
	private static Predicate<TripCard> hasTypes(TripSearchRequest request) {
		return card -> request.getType().isEmpty()
				|| request.getType().stream().anyMatch(type -> card.left(type) > 0);
	}

	/**
	 * Sold-out trips go last whatever the order, since nothing on them can be
	 * picked. Cheapest and fastest break ties by departure time.
	 */
	private static Comparator<TripCard> order(String sortKey) {
		Comparator<TripCard> byDeparture = Comparator.comparing(TripCard::departureAt);
		Comparator<TripCard> chosen = switch (sortKey) {
			case TripSearchRequest.SORT_CHEAPEST -> Comparator.comparingLong(TripCard::price).thenComparing(byDeparture);
			case TripSearchRequest.SORT_FASTEST -> Comparator
					.comparing((TripCard card) -> Duration.between(card.departureAt(), card.arrivalAt()))
					.thenComparing(byDeparture);
			default -> byDeparture;
		};
		return Comparator.comparing(TripCard::soldOut).thenComparing(chosen);
	}

}
