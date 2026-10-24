package com.voyagego.traingo.common.exception;

/** The route already has trips, so it cannot be deleted. */
public class RouteInUseException extends BusinessRuleException {

	private final long tripCount;

	public RouteInUseException(long tripCount) {
		super("Không xóa được: tuyến đã có " + tripCount + " chuyến.");
		this.tripCount = tripCount;
	}

	public long getTripCount() {
		return tripCount;
	}

}
