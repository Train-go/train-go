package com.voyagego.traingo.common.exception;

public class DuplicateRouteException extends RuntimeException {
	public DuplicateRouteException() {
		super("Tuyến giữa hai ga này đã có.");
	}

	public String getField() { return "destinationStationId"; }
}
