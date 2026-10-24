package com.voyagego.traingo.common.exception;

import com.voyagego.traingo.entity.Station;

/** The station is still the origin or destination of a route. */
public class StationInUseException extends BusinessRuleException {

	public StationInUseException(Station station) {
		super("Không xóa được " + station.getName() + " (" + station.getCode()
				+ ") vì ga đang là điểm đi hoặc điểm đến của một tuyến.");
	}

}
