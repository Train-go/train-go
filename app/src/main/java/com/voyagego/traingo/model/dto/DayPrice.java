package com.voyagego.traingo.model.dto;

import java.time.LocalDate;

/**
 * One day of the trip list's date strip for one pair of stations, read by
 * {@code TripRepository.findDayPrices}. A day with no bookable trip has no row.
 */
public interface DayPrice {

	LocalDate getDay();

	/** The lowest price among the day's trips that still have a free seat, or null when none has. */
	Long getMinPrice();

	long getTripCount();

	/** Free seats on all the day's trips. */
	long getAvailableSeats();

}
