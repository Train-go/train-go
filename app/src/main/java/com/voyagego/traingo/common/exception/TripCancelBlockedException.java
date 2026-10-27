package com.voyagego.traingo.common.exception;

/**
 * The trip is already cancelled, has departed or has confirmed bookings, so it
 * cannot be cancelled (D2). The reason is the sentence the trip sheet shows.
 */
public class TripCancelBlockedException extends BusinessRuleException {

	public TripCancelBlockedException(String reason) {
		super(reason);
	}

}
