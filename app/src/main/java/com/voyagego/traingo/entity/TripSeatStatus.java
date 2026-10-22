package com.voyagego.traingo.entity;

/**
 * Valid moves: AVAILABLE -> BOOKED when the booking is paid, BOOKED ->
 * AVAILABLE when the booking is cancelled. There is no hold: a seat is claimed
 * only inside the payment transaction. SELECTED exists only in the browser and
 * is never stored.
 */
public enum TripSeatStatus {
	AVAILABLE,
	BOOKED
}
