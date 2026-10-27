package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.voyagego.traingo.common.TextUtils;
import com.voyagego.traingo.common.exception.DuplicateRouteException;
import com.voyagego.traingo.common.exception.InvalidRouteBatchException;
import com.voyagego.traingo.common.exception.RouteInUseException;
import com.voyagego.traingo.common.exception.RouteNotFoundException;
import com.voyagego.traingo.common.exception.StationNotFoundException;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.request.RouteBatchRequest;
import com.voyagego.traingo.model.request.RouteCreateRequest;
import com.voyagego.traingo.model.response.RouteRow;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TripRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Service
@Validated
public class RouteService {
	private final RouteRepository routes;
	private final StationRepository stations;
	private final TripRepository trips;

	public RouteService(RouteRepository routes, StationRepository stations, TripRepository trips) {
		this.routes = routes;
		this.stations = stations;
		this.trips = trips;
	}

	@Transactional(readOnly = true)
	public List<RouteRow> findAll() {
		List<Route> all = routes.findAllWithStations();
		Map<Long, Long> totals = counts(trips.countByRoute());
		Map<Long, Long> upcoming = counts(trips.countUpcomingByRoute(LocalDateTime.now()));
		Set<StationPair> pairs = all.stream().map(route -> new StationPair(
				route.getOriginStation().getId(), route.getDestinationStation().getId())).collect(Collectors.toSet());
		return all.stream().map(route -> new RouteRow(route,
				upcoming.getOrDefault(route.getId(), 0L), totals.getOrDefault(route.getId(), 0L),
				pairs.contains(new StationPair(route.getDestinationStation().getId(),
						route.getOriginStation().getId())))).toList();
	}

	/** Search the display rows so filtering keeps their order and trip counts. */
	@Transactional(readOnly = true)
	public List<RouteRow> search(String q) {
		List<RouteRow> all = findAll();
		String query = TextUtils.fold(q == null ? "" : q.strip());
		if (query.isBlank()) {
			return all;
		}
		return all.stream().filter(row -> TextUtils.fold(row.getOriginName()).contains(query)
				|| TextUtils.fold(row.getOriginCode()).contains(query)
				|| TextUtils.fold(row.getDestinationName()).contains(query)
				|| TextUtils.fold(row.getDestinationCode()).contains(query)).toList();
	}

	@Transactional(readOnly = true)
	public RouteRow findRow(long id) {
		Route route = routes.findWithStationsById(id).orElseThrow(() -> new RouteNotFoundException(id));
		long upcoming = counts(trips.countUpcomingByRoute(LocalDateTime.now())).getOrDefault(id, 0L);
		return new RouteRow(route, upcoming, trips.countByRouteId(id),
				routes.existsByOriginStationIdAndDestinationStationId(
						route.getDestinationStation().getId(), route.getOriginStation().getId()));
	}

	@Transactional
	public List<Route> create(@NotNull @Valid RouteCreateRequest form) {
		long originId = form.getOriginStationId();
		long destinationId = form.getDestinationStationId();
		// Ordered endpoint locks serialize creations in either direction, including
		// when neither route exists yet. Never take the inverse lock order.
		Station first = lockStation(Math.min(originId, destinationId));
		Station second = lockStation(Math.max(originId, destinationId));
		Station origin = originId == first.getId() ? first : second;
		Station destination = destinationId == first.getId() ? first : second;
		if (routes.existsByOriginStationIdAndDestinationStationId(originId, destinationId)) {
			throw new DuplicateRouteException();
		}
		List<Route> created = new ArrayList<>();
		created.add(routes.save(new Route(origin, destination, form.getDistanceKm())));
		if (form.isBothWays()
				&& !routes.existsByOriginStationIdAndDestinationStationId(destinationId, originId)) {
			created.add(routes.save(new Route(destination, origin, form.getDistanceKm())));
		}
		// Both inserts must succeed in this transaction, or neither is kept.
		routes.flush();
		return List.copyOf(created);
	}

	/**
	 * Creates every pair that does not exist yet, in one transaction. A pair
	 * whose direction already exists is skipped, not an error; a station that
	 * does not exist fails the whole batch and nothing is created. Stations are
	 * locked in id order, like {@link #create}, so two batches cannot deadlock.
	 *
	 * @return only the routes this call created
	 */
	@Transactional
	public List<Route> createMany(@NotNull @Valid RouteBatchRequest request) {
		Map<StationPair, Integer> wanted = new LinkedHashMap<>();
		for (RouteBatchRequest.RoutePair pair : request.getRoutes()) {
			wanted.putIfAbsent(new StationPair(pair.getOriginStationId(), pair.getDestinationStationId()),
					pair.getDistanceKm());
		}
		Map<Long, Station> locked = new TreeMap<>();
		wanted.keySet().forEach(pair -> {
			locked.put(pair.originId(), null);
			locked.put(pair.destinationId(), null);
		});
		locked.replaceAll((id, none) -> lockStation(id));

		List<Route> created = new ArrayList<>();
		for (Map.Entry<StationPair, Integer> entry : wanted.entrySet()) {
			StationPair pair = entry.getKey();
			if (routes.existsByOriginStationIdAndDestinationStationId(pair.originId(), pair.destinationId())) {
				continue;
			}
			created.add(routes.save(new Route(locked.get(pair.originId()), locked.get(pair.destinationId()),
					entry.getValue())));
		}
		routes.flush();
		return List.copyOf(created);
	}

	/**
	 * Deletes several routes at once, for "Hoàn tác" after a batch. All or none:
	 * one route that has trips keeps every route.
	 */
	@Transactional
	public void deleteMany(List<Long> ids) {
		if (ids == null || ids.isEmpty() || ids.size() > RouteBatchRequest.MAX_ROUTES) {
			throw new InvalidRouteBatchException("Chọn từ 1 đến " + RouteBatchRequest.MAX_ROUTES + " tuyến.");
		}
		List<Route> found = new ArrayList<>();
		for (Long id : new TreeSet<>(ids)) {
			found.add(lockRoute(id));
		}
		for (Route route : found) {
			long count = trips.countByRouteId(route.getId());
			if (count > 0) {
				throw new RouteInUseException(count);
			}
		}
		routes.deleteAll(found);
		routes.flush();
	}

	@Transactional
	public Route changeDistance(long id,
			@Positive(message = "Khoảng cách phải lớn hơn 0 km.") int km) {
		Route route = lockRoute(id);
		route.changeDistance(km);
		routes.flush();
		return route;
	}

	@Transactional
	public void delete(long id) {
		// A trip insert needs a shared FK lock on this row. Holding the write lock
		// prevents a new reference between the count and the delete.
		Route route = lockRoute(id);
		long count = trips.countByRouteId(id);
		if (count > 0) {
			throw new RouteInUseException(count);
		}
		// Check first: catching an FK error and continuing inside the transaction
		// would leave it rollback-only instead of producing a domain error.
		routes.delete(route);
		routes.flush();
	}

	@Transactional(readOnly = true)
	public int suggestedDistance(long originId, long destinationId) {
		Station origin = stations.findById(originId).orElseThrow(() -> new StationNotFoundException());
		Station destination = stations.findById(destinationId)
				.orElseThrow(() -> new StationNotFoundException());
		return Math.abs(origin.getKmMark() - destination.getKmMark());
	}

	private Station lockStation(long id) {
		return stations.findByIdForUpdate(id).orElseThrow(() -> new StationNotFoundException());
	}

	private Route lockRoute(long id) {
		return routes.findByIdForUpdate(id).orElseThrow(() -> new RouteNotFoundException(id));
	}

	private static Map<Long, Long> counts(List<IdCount> counts) {
		return counts.stream().collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
	}

	private record StationPair(long originId, long destinationId) {}
}
