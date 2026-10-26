package com.voyagego.traingo.common.exception;

/** The station is still the origin or destination of a route. */
public class StationInUseException extends BusinessRuleException {

	public StationInUseException(long routeCount) {
		super("Không xóa được: ga đang có " + routeCount + " tuyến. Xóa các tuyến đó trước.");
	}

}
