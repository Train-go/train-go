package com.voyagego.traingo.common.exception;

/**
 * A booking cannot be cancelled: it is already cancelled, or its trip has
 * departed. The message says which, and is shown to the user.
 */
public class BookingCancelBlockedException extends BusinessRuleException {

	public BookingCancelBlockedException(String message) {
		super(message);
	}

}
