package com.voyagego.traingo.model.dto;

import java.time.LocalDate;

/**
 * Confirmed bookings paid on one day, for the overview's sales chart. A day
 * with no sale has no row.
 */
public interface DayTotal {

	LocalDate getDay();

	long getBookings();

	/** Sum of the bookings' total_price, in dong. */
	long getRevenue();

}
