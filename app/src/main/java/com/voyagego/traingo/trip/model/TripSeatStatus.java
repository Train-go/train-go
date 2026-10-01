package com.voyagego.traingo.trip.model;

/**
 * Valid moves: AVAILABLE -> HELD -> BOOKED on payment, HELD -> AVAILABLE when
 * the hold expires, BOOKED -> AVAILABLE when the booking is cancelled.
 * SELECTED exists only in the browser and is never stored.
 */
public enum TripSeatStatus {
	AVAILABLE,
	HELD,
	BOOKED
}
