package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.TextUtils;
import com.voyagego.traingo.common.exception.TripCancelBlockedException;
import com.voyagego.traingo.common.exception.TripNotFoundException;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.CoachType;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.TripDetail;
import com.voyagego.traingo.model.response.TripListView;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * Trip rules of docs/product/admin.md, "Quản lý chuyến", for the trip sheet
 * and /admin/trips. Every rule compares the departure with {@code now} (D8),
 * which the caller passes in so a rule reads the same clock for the whole
 * request.
 *
 * Trips are created on their train's page, not here. They are never deleted:
 * only a SCHEDULED trip that has not departed and has no CONFIRMED booking
 * can be cancelled (D2), because cancelling a paid trip needs a refund the
 * project does not have.
 */
@Service
public class TripAdminService {

	public static final String TAB_UPCOMING = "upcoming";
	public static final String TAB_DEPARTED = "departed";
	public static final String TAB_CANCELLED = "cancelled";

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	/** Indexed by DayOfWeek.getValue() - 1, Monday first. */
	private static final String[] WEEKDAYS = { "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy",
			"Chủ nhật" };
	private static final Sort EARLIEST_FIRST = Sort.by("departureAt").ascending();
	private static final Sort LATEST_FIRST = Sort.by("departureAt").descending();

	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;
	private final TicketRepository ticketRepository;
	private final BookingRepository bookingRepository;
	private final TrainRepository trainRepository;

	public TripAdminService(TripRepository tripRepository, TripSeatRepository tripSeatRepository,
			TicketRepository ticketRepository, BookingRepository bookingRepository,
			TrainRepository trainRepository) {
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
		this.ticketRepository = ticketRepository;
		this.bookingRepository = bookingRepository;
		this.trainRepository = trainRepository;
	}

	@Transactional(readOnly = true)
	public TripDetail detail(long tripId, LocalDateTime now) {
		Trip trip = tripRepository.findDetailById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
		Map<Long, Ticket> ticketBySeat = ticketRepository.findConfirmedByTrip(tripId).stream()
				.collect(Collectors.toMap(ticket -> ticket.getTripSeat().getId(), Function.identity()));
		List<TripSeat> seats = tripSeatRepository.findByTripIdOrderByCoachNumberAscSeatCodeAsc(tripId);

		Map<Integer, List<TripSeat>> seatsByCoach = seats.stream()
				.collect(Collectors.groupingBy(TripSeat::getCoachNumber, LinkedHashMap::new, Collectors.toList()));
		List<TripDetail.CoachView> coaches = new ArrayList<>();
		seatsByCoach.forEach((number, coachSeats) -> {
			List<TripDetail.SeatView> views = coachSeats.stream().map(seat -> {
				Ticket ticket = ticketBySeat.get(seat.getId());
				return new TripDetail.SeatView(seat.getSeatCode(), seat.getCabinNumber(), ticket != null,
						ticket == null ? null : ticket.getFullName(),
						ticket == null ? null : ticket.getBooking().getBookingCode());
			}).toList();
			CoachType type = coachSeats.get(0).getCoachType();
			int sold = (int) views.stream().filter(TripDetail.SeatView::isSold).count();
			coaches.add(new TripDetail.CoachView(number, type.name(), coachTypeLabel(type), views.size(), sold, views));
		});

		List<TripDetail.PassengerRow> passengers = ticketBySeat.values().stream()
				.sorted(Comparator.comparingInt((Ticket ticket) -> ticket.getTripSeat().getCoachNumber())
						.thenComparing(ticket -> ticket.getTripSeat().getSeatCode()))
				.map(ticket -> new TripDetail.PassengerRow(ticket.getFullName(),
						TextUtils.maskId(ticket.getIdentityNumber()), ticket.getTripSeat().getCoachNumber(),
						ticket.getTripSeat().getSeatCode(), ticket.getBooking().getBookingCode(),
						ticket.getTicketPrice()))
				.toList();

		long bookedCount = bookingRepository.countByTripIdAndBookingStatus(tripId, BookingStatus.CONFIRMED);
		return new TripDetail(trip.getId(), trip.getTrain().getId(), trip.getTrain().getCode(),
				trip.getTrain().getName(), trip.getRoute().getOriginStation().getName(),
				trip.getRoute().getDestinationStation().getName(), phaseCss(trip, now), trip.phaseLabel(now),
				WEEKDAYS[trip.getDepartureAt().getDayOfWeek().getValue() - 1] + ", "
						+ trip.getDepartureAt().format(DATE) + " · " + timeRange(trip),
				trip.getDepartureAt().format(TIME) + " " + trip.getDepartureAt().format(DAY_MONTH),
				trip.getRoute().getDistanceKm(), duration(trip), trip.getBasePrice(), passengers.size(), seats.size(),
				bookedCount, trip.isCancelled(), cancelBlockReason(trip, bookedCount, now),
				coaches, passengers);
	}

	/** Why the trip cannot be cancelled now, or null when it can. */
	@Transactional(readOnly = true)
	public String cancelBlockReason(Trip trip, LocalDateTime now) {
		return cancelBlockReason(trip,
				bookingRepository.countByTripIdAndBookingStatus(trip.getId(), BookingStatus.CONFIRMED), now);
	}

	/**
	 * The rules are checked again here, not trusted from the sheet: the page
	 * may be stale. A booking insert takes a shared lock on its trip row
	 * through the foreign key, so holding the write lock keeps a payment from
	 * confirming a booking between the check and the cancel.
	 */
	@Transactional
	public Trip cancel(long tripId, LocalDateTime now) {
		Trip trip = tripRepository.findByIdForUpdate(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
		String reason = cancelBlockReason(trip, now);
		if (reason != null) {
			throw new TripCancelBlockedException(reason);
		}
		trip.cancel();
		return trip;
	}

	/**
	 * One group of /admin/trips. An unknown tab falls back to upcoming. The
	 * station search runs here rather than in SQL because it ignores
	 * Vietnamese marks ("hue" finds "Huế"), which MySQL's collation does not
	 * do for every letter.
	 */
	@Transactional(readOnly = true)
	public TripListView list(String tab, Long trainId, String q, LocalDateTime now) {
		String selected = switch (tab == null ? "" : tab) {
			case TAB_DEPARTED, TAB_CANCELLED -> tab;
			default -> TAB_UPCOMING;
		};
		List<Trip> trips = switch (selected) {
			case TAB_DEPARTED -> tripRepository.findDeparted(now, trainId, LATEST_FIRST);
			case TAB_CANCELLED -> tripRepository.findCancelled(trainId, LATEST_FIRST);
			default -> tripRepository.findUpcoming(now, trainId, EARLIEST_FIRST);
		};
		String needle = TextUtils.fold(q).strip();
		if (!needle.isEmpty()) {
			trips = trips.stream().filter(trip -> matches(trip, needle)).toList();
		}

		Map<Long, Long> soldByTrip = ticketRepository.countConfirmedByTrip().stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		int totalSeats = CoachLayout.totalSeats();
		Map<LocalDate, List<TripListView.Row>> rowsByDay = new LinkedHashMap<>();
		for (Trip trip : trips) {
			rowsByDay.computeIfAbsent(trip.getDepartureAt().toLocalDate(), day -> new ArrayList<>())
					.add(new TripListView.Row(trip.getId(), timeRange(trip), trip.getTrain().getCode(),
							trip.getRoute().getOriginStation().getName() + " → "
									+ trip.getRoute().getDestinationStation().getName(),
							trip.getBasePrice(), soldByTrip.getOrDefault(trip.getId(), 0L), totalSeats,
							phaseCss(trip, now), trip.phaseLabel(now)));
		}
		List<TripListView.DayGroup> days = rowsByDay.entrySet().stream()
				.map(entry -> new TripListView.DayGroup(dayLabel(entry.getKey(), now.toLocalDate()),
						entry.getKey().format(DATE), entry.getValue()))
				.toList();

		// Deleted trains keep their trips in the list but are not offered as a filter.
		List<TripListView.TrainOption> trains = trainRepository.findAllByDeletedFalseOrderByCodeAsc().stream()
				.map(train -> new TripListView.TrainOption(train.getId(), train.getCode(), train.getName()))
				.toList();
		return new TripListView(selected, trainId, needle.isEmpty() ? null : q.strip(),
				tripRepository.countUpcoming(now), tripRepository.countDeparted(now),
				tripRepository.countByStatus(TripStatus.CANCELLED), trains, days);
	}

	/** The sentences of the sheet's footer (AC4), checked in the mockup's order. */
	private static String cancelBlockReason(Trip trip, long bookedCount, LocalDateTime now) {
		if (trip.isCancelled()) {
			return "Chuyến đã hủy, chỗ không còn bán.";
		}
		if (trip.getStatus() != TripStatus.SCHEDULED || trip.hasDeparted(now)) {
			return "Không hủy được: chuyến đã khởi hành.";
		}
		if (bookedCount > 0) {
			return "Không hủy được: chuyến đã có " + bookedCount + " booking. Hủy các booking đó trước.";
		}
		return null;
	}

	private static boolean matches(Trip trip, String needle) {
		Station origin = trip.getRoute().getOriginStation();
		Station destination = trip.getRoute().getDestinationStation();
		return TextUtils.fold(origin.getName() + " " + origin.getCode() + " " + destination.getName() + " "
				+ destination.getCode()).contains(needle);
	}

	/** "08:00 → 16:45", or "22:00 → 06:10 27/10" when the trip arrives on a later day. */
	private static String timeRange(Trip trip) {
		String arrival = trip.getArrivalAt().format(TIME);
		if (!trip.getArrivalAt().toLocalDate().equals(trip.getDepartureAt().toLocalDate())) {
			arrival += " " + trip.getArrivalAt().format(DAY_MONTH);
		}
		return trip.getDepartureAt().format(TIME) + " → " + arrival;
	}

	private static String duration(Trip trip) {
		long minutes = Duration.between(trip.getDepartureAt(), trip.getArrivalAt()).toMinutes();
		long hours = minutes / 60;
		long rest = minutes % 60;
		if (hours == 0) {
			return rest + " phút";
		}
		return rest == 0 ? hours + " giờ" : hours + " giờ " + String.format(Locale.ROOT, "%02d", rest) + " phút";
	}

	private static String dayLabel(LocalDate day, LocalDate today) {
		if (day.equals(today)) {
			return "Hôm nay";
		}
		if (day.equals(today.plusDays(1))) {
			return "Ngày mai";
		}
		if (day.equals(today.minusDays(1))) {
			return "Hôm qua";
		}
		return WEEKDAYS[day.getDayOfWeek().getValue() - 1];
	}

	private static String coachTypeLabel(CoachType type) {
		return type == CoachType.SLEEPER ? "Giường nằm" : "Ghế ngồi";
	}

	/**
	 * The .tag modifier of {@link Trip#phaseLabel}, as on the train page
	 * (TrainScheduleService), so a trip has the same colour on both pages.
	 */
	private static String phaseCss(Trip trip, LocalDateTime now) {
		if (trip.isCancelled()) {
			return "cancelled";
		}
		if (!trip.hasDeparted(now)) {
			return "scheduled";
		}
		return trip.isRunning(now) ? "running" : "done";
	}

}
