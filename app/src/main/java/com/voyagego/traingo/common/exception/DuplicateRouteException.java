package com.voyagego.traingo.common.exception;

public class DuplicateRouteException extends BusinessRuleException {

	public DuplicateRouteException() {
		super("Tuyến giữa hai ga này đã có.", "destinationStationId");
	}

}
