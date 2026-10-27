package com.voyagego.traingo.common.exception;

/**
 * The trip cannot be booked: it does not exist, was cancelled or has left.
 * The page handler shows the sentence on error/trip-unavailable with a link
 * back to the trip list; the API answers 409 with it.
 */
public class TripNotBookableException extends BusinessRuleException {

	public TripNotBookableException(String message) {
		super(message);
	}

	public static TripNotBookableException missing() {
		return new TripNotBookableException("Chuyến này không tồn tại.");
	}

	public static TripNotBookableException cancelled() {
		return new TripNotBookableException("Chuyến này đã bị hủy.");
	}

	public static TripNotBookableException departed() {
		return new TripNotBookableException("Chuyến này đã khởi hành.");
	}

}
