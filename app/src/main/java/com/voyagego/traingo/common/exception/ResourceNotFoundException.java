package com.voyagego.traingo.common.exception;

/**
 * Something the request names does not exist: a station, a trip, a booking.
 * Pages answer with the 404 page and the API with a 404 ApiError, so a
 * feature only throws its own subclass and never sets a status itself.
 *
 * The message is shown to the user, so it is written in Vietnamese.
 */
public abstract class ResourceNotFoundException extends RuntimeException {

	protected ResourceNotFoundException(String message) {
		super(message);
	}

}
