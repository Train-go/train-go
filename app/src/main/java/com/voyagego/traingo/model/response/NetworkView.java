package com.voyagego.traingo.model.response;

import java.util.List;

public record NetworkView(List<StationNode> stations, List<RouteEdge> routes) {

	public record StationNode(long id, String code, String name, String city, String address,
			int kmMark, long routeCount) {}

	public record RouteEdge(long id, long originId, String originCode, long destinationId,
			String destinationCode, int distanceKm, long upcomingTrips, long totalTrips) {}

}
