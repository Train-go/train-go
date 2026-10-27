package com.voyagego.traingo.model.dto;

import com.voyagego.traingo.entity.CoachType;

/**
 * One row of the free-seat count on the trip list: how many seats of one
 * coach type are still AVAILABLE on one trip. Spring Data fills it from the
 * aliases {@code tripId}, {@code coachType} and {@code available}.
 */
public interface SeatAvailability {

	Long getTripId();

	CoachType getCoachType();

	long getAvailable();

}
