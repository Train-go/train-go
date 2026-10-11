package com.voyagego.traingo.entity;

/**
 * There is no COMPLETED: a CONFIRMED booking whose trip is COMPLETED is shown
 * in the completed group by the UI.
 */
public enum BookingStatus {
	PENDING,
	CONFIRMED,
	CANCELLED
}
