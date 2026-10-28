package com.voyagego.traingo.model.dto;

import java.time.LocalDate;

/**
 * What is already booked for the trips that leave on one day, for the
 * overview's forecast chart. Only confirmed bookings count.
 */
public interface DayForecast {

	LocalDate getDay();

	long getTrips();

	long getSoldSeats();

	/** Sum of the tickets' price, in dong. */
	long getRevenue();

}
