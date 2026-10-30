package com.voyagego.traingo.model.dto;

/** Booked seats of one coach of one trip. */
public interface TripCoachCount {

	Long getTripId();

	Integer getCoachNumber();

	long getTotal();

}
