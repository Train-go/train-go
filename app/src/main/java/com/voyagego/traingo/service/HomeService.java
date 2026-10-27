package com.voyagego.traingo.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.Destination;
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.model.response.StationOption;
import com.voyagego.traingo.model.response.StationPhoto;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TripRepository;

/**
 * Data for the landing page: the stations of the search form, the destination
 * cards and the popular route cards. The form and the cards hand station
 * codes to the trip list, /trips (issue #26).
 */
@Service
@Transactional(readOnly = true)
public class HomeService {

	private static final int POPULAR_ROUTE_COUNT = 4;

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

	/** The "Đi đâu cũng có tàu" block: the first card is the large one. */
	private static final List<String> DESTINATION_CODES = List.of("NTR", "DNG", "HUE", "HNI", "SGN");

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

	/** The destination cards, skipping any station no longer in the database. */
	public List<Destination> destinations() {
		Map<String, Station> byCode = stationRepository.findAll().stream()
				.collect(Collectors.toMap(Station::getCode, station -> station));
		return DESTINATION_CODES.stream().filter(byCode::containsKey).map(byCode::get)
				.map(station -> new Destination(station.getCode(), station.getName(),
						PopularRoute.placeName(station.getName()), PHOTOS.get(station.getCode())))
				.toList();
	}

	/**
	 * The routes with the most bookings, cancelled ones not counted. Routes with
	 * the same number of bookings go to the one with more trips, then to the
	 * lower id, so the order never changes between two page loads. The card's
	 * note is the distance, which is all the database knows about a route.
	 */
	public List<PopularRoute> popularRoutes() {
		Map<Long, Long> bookingsByRoute = toMap(bookingRepository.countByRouteExcludingStatus(BookingStatus.CANCELLED));
		Map<Long, Long> tripsByRoute = toMap(tripRepository.countByRoute());
		Comparator<Route> mostPopularFirst = Comparator
				.comparing((Route route) -> bookingsByRoute.getOrDefault(route.getId(), 0L)).reversed()
				.thenComparing(Comparator.comparing((Route route) -> tripsByRoute.getOrDefault(route.getId(), 0L))
						.reversed())
				.thenComparing(Route::getId);
		return routeRepository.findAllWithStations().stream().sorted(mostPopularFirst).limit(POPULAR_ROUTE_COUNT)
				.map(route -> new PopularRoute(route.getOriginStation().getCode(), route.getOriginStation().getName(),
						route.getDestinationStation().getCode(), route.getDestinationStation().getName(),
						route.getDistanceKm() + " km", PHOTOS.get(route.getDestinationStation().getCode())))
				.toList();
	}

	private static Map<Long, Long> toMap(List<IdCount> counts) {
		return counts.stream().collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
	}

}
