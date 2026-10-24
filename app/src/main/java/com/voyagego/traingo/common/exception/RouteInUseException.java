package com.voyagego.traingo.common.exception;

public class RouteInUseException extends RuntimeException {
	private final long tripCount;

	public RouteInUseException(long tripCount) {
		super("Không xóa được: tuyến đã có " + tripCount + " chuyến.");
		this.tripCount = tripCount;
	}

	public long getTripCount() { return tripCount; }
	public String getField() { return null; }
}
