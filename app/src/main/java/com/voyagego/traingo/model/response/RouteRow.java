package com.voyagego.traingo.model.response;

import com.voyagego.traingo.entity.Route;

/** Detached data for the route list and station matrix. */
public class RouteRow {
	private final Long id;
	private final Long originStationId;
	private final String originCode;
	private final String originName;
	private final int originKmMark;
	private final Long destinationStationId;
	private final String destinationCode;
	private final String destinationName;
	private final int destinationKmMark;
	private final int distanceKm;
	private final long upcomingTrips;
	private final long totalTrips;
	private final boolean hasReverse;

	public RouteRow(Route route, long upcomingTrips, long totalTrips, boolean hasReverse) {
		var origin = route.getOriginStation();
		var destination = route.getDestinationStation();
		this.id = route.getId();
		this.originStationId = origin.getId();
		this.originCode = origin.getCode();
		this.originName = origin.getName();
		this.originKmMark = origin.getKmMark();
		this.destinationStationId = destination.getId();
		this.destinationCode = destination.getCode();
		this.destinationName = destination.getName();
		this.destinationKmMark = destination.getKmMark();
		this.distanceKm = route.getDistanceKm();
		this.upcomingTrips = upcomingTrips;
		this.totalTrips = totalTrips;
		this.hasReverse = hasReverse;
	}

	public Long getId() { return id; }
	public Long getOriginStationId() { return originStationId; }
	public String getOriginCode() { return originCode; }
	public String getOriginName() { return originName; }
	public int getOriginKmMark() { return originKmMark; }
	public Long getDestinationStationId() { return destinationStationId; }
	public String getDestinationCode() { return destinationCode; }
	public String getDestinationName() { return destinationName; }
	public int getDestinationKmMark() { return destinationKmMark; }
	public int getDistanceKm() { return distanceKm; }
	public long getUpcomingTrips() { return upcomingTrips; }
	public long getTotalTrips() { return totalTrips; }
	public boolean isHasReverse() { return hasReverse; }
}
