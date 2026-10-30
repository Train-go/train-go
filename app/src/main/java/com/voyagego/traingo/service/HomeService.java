package com.voyagego.traingo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.model.response.PopularView;
import com.voyagego.traingo.model.response.StationOption;
import com.voyagego.traingo.model.response.StationPhoto;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TripRepository;

/**
 * Data for the landing page: the stations of the search form and the journeys. The form and the cards hand station
 * codes to the trip list, /trips (issue #26).
 */
@Service
@Transactional(readOnly = true)
public class HomeService {

	/**
	 * One photo per station, by station code. Keyed by code rather than name so
	 * renaming a station in the admin keeps its photo. A station added later
	 * simply has no photo until one is added here and credited in the footer.
	 */
	private static final Map<String, StationPhoto> PHOTOS = Map.of(
			"HNI", new StationPhoto("ga-ha-noi.jpg", "Hồ Hoàn Kiếm, Hà Nội"),
			"THA", new StationPhoto("ga-thanh-hoa.jpg", "Bãi biển Sầm Sơn, Thanh Hóa"),
			"VIN", new StationPhoto("ga-vinh.jpg", "Thành phố Vinh nhìn từ núi Quyết"),
			"DHO", new StationPhoto("ga-dong-hoi.jpg", "Sân ga Đồng Hới lúc chiều tối"),
			"HUE", new StationPhoto("ga-hue.jpg", "Cổng Đại Nội Huế"),
			"DNG", new StationPhoto("ga-da-nang.jpg", "Cầu Rồng Đà Nẵng về đêm"),
			"NTR", new StationPhoto("ga-nha-trang.jpg", "Bờ biển Nha Trang"),
			"SGN", new StationPhoto("ga-sai-gon.jpg", "Sài Gòn về đêm"));

	/** The region of a destination, by station code, for the chips of the journeys block. */
	private static final Map<String, String> REGIONS = Map.of("HNI", "bac", "THA", "bac", "VIN", "trung", "DHO", "trung",
			"HUE", "trung", "DNG", "trung", "NTR", "trung", "SGN", "nam");
	private static final Map<String, String> REGION_NAMES = new LinkedHashMap<>();
	static {
		REGION_NAMES.put("bac", "Miền Bắc");
		REGION_NAMES.put("trung", "Miền Trung");
		REGION_NAMES.put("nam", "Miền Nam");
	}
	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	private static final int COMING_DAYS = 14;
	public static final int FIRST_PAGE = 6;
	private static final int MAX_SHOWN = 200;

	private final StationRepository stationRepository;
	private final RouteRepository routeRepository;
	private final TripRepository tripRepository;
	private final BookingRepository bookingRepository;

	public HomeService(StationRepository stationRepository, RouteRepository routeRepository,
			TripRepository tripRepository, BookingRepository bookingRepository) {
		this.stationRepository = stationRepository;
		this.routeRepository = routeRepository;
		this.tripRepository = tripRepository;
		this.bookingRepository = bookingRepository;
	}

	/** North to south along the line, the order a traveller reads a map in. */
	public List<StationOption> stations() {
		return stationRepository.findAllByOrderByKmMarkAsc().stream()
				.map(station -> new StationOption(station.getCode(), station.getName()))
				.toList();
	}

	/**
	 * The routes with the most bookings, cancelled ones not counted, that have a
	 * trip in the next two weeks; a route with no coming trip has nothing to show.
	 * Routes with the same number of bookings go to the one with more trips, then
	 * to the lower id, so the order never changes between two page loads.
	 */
	public PopularView popular(String region, int limit, LocalDateTime now) {
		Map<Long, Long> bookingsByRoute = toMap(bookingRepository.countByRouteExcludingStatus(BookingStatus.CANCELLED));
		Map<Long, Long> tripsByRoute = toMap(tripRepository.countByRoute());
		Comparator<Route> mostPopularFirst = Comparator
				.comparing((Route route) -> bookingsByRoute.getOrDefault(route.getId(), 0L)).reversed()
				.thenComparing(Comparator.comparing((Route route) -> tripsByRoute.getOrDefault(route.getId(), 0L))
						.reversed())
				.thenComparing(Route::getId);
		// In departure order, so the first of a route is its next one.
		Map<Long, List<Trip>> coming = tripRepository.findUpcomingBetween(now, now.plusDays(COMING_DAYS)).stream()
				.collect(Collectors.groupingBy(trip -> trip.getRoute().getId()));
		List<Route> ranked = routeRepository.findAllWithStations().stream().filter(route -> coming.containsKey(route.getId()))
				.sorted(mostPopularFirst).toList();

		String chosen = REGION_NAMES.containsKey(region) ? region : "";
		List<Route> inRegion = ranked.stream()
				.filter(route -> chosen.isEmpty() || chosen.equals(REGIONS.get(route.getDestinationStation().getCode())))
				.toList();
		int shown = Math.max(FIRST_PAGE, Math.min(limit, MAX_SHOWN));
		List<PopularView.Chip> chips = new ArrayList<>(List.of(new PopularView.Chip("", "Tất cả")));
		REGION_NAMES.forEach((key, label) -> chips.add(new PopularView.Chip(key, label)));
		List<PopularRoute> top = ranked.stream().limit(4).map(route -> toCard(route, coming.get(route.getId()), now)).toList();
		return new PopularView(chosen, shown, inRegion.size() > shown, chips,
				inRegion.stream().limit(shown).map(route -> toCard(route, coming.get(route.getId()), now)).toList(),
				top.stream().limit(3).toList(), top, top.isEmpty() ? null : top.get(0), nextDays(now.toLocalDate()));
	}

	private static PopularRoute toCard(Route route, List<Trip> trips, LocalDateTime now) {
		Trip next = trips.get(0);
		long price = trips.stream().mapToLong(Trip::getBasePrice).min().orElse(next.getBasePrice());
		return new PopularRoute(route.getOriginStation().getCode(), route.getOriginStation().getName(),
				route.getDestinationStation().getCode(), route.getDestinationStation().getName(),
				route.getDistanceKm() + " km", PHOTOS.get(route.getDestinationStation().getCode()),
				TripAdminService.duration(next), nextLabel(next.getDepartureAt(), now.toLocalDate()), price);
	}

	/** "Hôm nay, 08:00", "Ngày mai, 08:00" or "T4 28/10, 08:00". */
	private static String nextLabel(LocalDateTime departure, LocalDate today) {
		LocalDate day = departure.toLocalDate();
		String time = departure.format(TIME);
		if (day.equals(today)) {
			return "Hôm nay, " + time;
		}
		if (day.equals(today.plusDays(1))) {
			return "Ngày mai, " + time;
		}
		return SHORT_WEEKDAYS[day.getDayOfWeek().getValue() - 1] + " " + day.format(DAY_MONTH) + ", " + time;
	}

	private static List<PopularView.Day> nextDays(LocalDate today) {
		List<PopularView.Day> days = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			LocalDate day = today.plusDays(i);
			days.add(new PopularView.Day(day, i == 0 ? "Hôm nay" : SHORT_WEEKDAYS[day.getDayOfWeek().getValue() - 1],
					day.format(DAY_MONTH)));
		}
		return days;
	}

	private static Map<Long, Long> toMap(List<IdCount> counts) {
		return counts.stream().collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
	}

}
