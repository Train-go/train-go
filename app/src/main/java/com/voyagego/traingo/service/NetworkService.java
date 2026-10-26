package com.voyagego.traingo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.response.NetworkView;
import com.voyagego.traingo.model.response.NetworkView.RouteEdge;
import com.voyagego.traingo.model.response.NetworkView.StationNode;
import com.voyagego.traingo.model.response.RouteRow;

@Service
public class NetworkService {

	private final StationService stationService;
	private final RouteService routeService;

	public NetworkService(StationService stationService, RouteService routeService) {
		this.stationService = stationService;
		this.routeService = routeService;
	}

	@Transactional(readOnly = true)
	public NetworkView network() {
		List<Station> stations = stationService.findAll();
		List<RouteRow> routes = routeService.findAll();
		Map<Long, Long> routeCounts = new HashMap<>();
		for (RouteRow route : routes) {
			routeCounts.merge(route.getOriginStationId(), 1L, Long::sum);
			routeCounts.merge(route.getDestinationStationId(), 1L, Long::sum);
		}
		List<StationNode> nodes = stations.stream()
				.map(station -> toNode(station, routeCounts.getOrDefault(station.getId(), 0L))).toList();
		List<RouteEdge> edges = routes.stream().map(route -> new RouteEdge(route.getId(),
				route.getOriginStationId(), route.getOriginCode(), route.getDestinationStationId(),
				route.getDestinationCode(), route.getDistanceKm(), route.getUpcomingTrips(), route.getTotalTrips()))
				.toList();
		return new NetworkView(nodes, edges);
	}

	@Transactional(readOnly = true)
	public StationNode stationNode(Station station) {
		return toNode(station, stationService.countRoutes(station.getId()));
	}

	private static StationNode toNode(Station station, long routeCount) {
		return new StationNode(station.getId(), station.getCode(), station.getName(), station.getCity(),
				station.getAddress(), station.getKmMark(), routeCount);
	}

}
