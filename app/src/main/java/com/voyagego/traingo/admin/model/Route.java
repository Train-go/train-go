package com.voyagego.traingo.admin.model;

public class Route {

	private final String code;
	private final String origin;
	private final String destination;
	private final int distanceKm;

	public Route(String code, String origin, String destination, int distanceKm) {
		this.code = code;
		this.origin = origin;
		this.destination = destination;
		this.distanceKm = distanceKm;
	}

	public String getCode() {
		return code;
	}

	public String getOrigin() {
		return origin;
	}

	public String getDestination() {
		return destination;
	}

	public int getDistanceKm() {
		return distanceKm;
	}

}
