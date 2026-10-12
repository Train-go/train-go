package com.voyagego.traingo.common.exception;

import com.voyagego.traingo.entity.Station;

/** The station is still the origin or destination of a route. */
public class StationInUseException extends RuntimeException {

	private final transient Station station;

	public StationInUseException(Station station) {
		super("Station " + station.getCode() + " is used by a route");
		this.station = station;
	}

	public Station getStation() {
		return station;
	}

}
