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
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TripRepository;

/**
 * Data for the landing page: the station names of the search form and the
 * popular route cards. The form still submits a station name; trip search
 * (stage 3 of docs/plans/active/mvp-traingo.md) decides whether it submits an
 * id or a code instead.
 */
@Service
@Transactional(readOnly = true)
public class HomeService {

	private static final int POPULAR_ROUTE_COUNT = 4;

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

	public List<String> stations() {
		return stationRepository.findAllByOrderByNameAsc().stream().map(Station::getName).toList();
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
				.map(route -> new PopularRoute(route.getOriginStation().getName(),
						route.getDestinationStation().getName(), route.getDistanceKm() + " km"))
				.toList();
	}

	private static Map<Long, Long> toMap(List<IdCount> counts) {
		return counts.stream().collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
	}

}
