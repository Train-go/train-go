package com.voyagego.traingo.model.response;

import java.time.LocalDateTime;

import com.voyagego.traingo.entity.CoachType;

/**
 * One trip on the customer's trip list. Every seat on a trip costs the same,
 * {@code trips.base_price}, so the card carries one price.
 *
 * @param durationLabel  running time as "1g45", or "8g" on the hour
 * @param arrivesNextDay days between the departure date and the arrival date,
 *                       0 for a same-day arrival
 */
public record TripCard(Long tripId, String trainCode, LocalDateTime departureAt, LocalDateTime arrivalAt,
		String durationLabel, int arrivesNextDay, long price, long seatsLeft, long sleepersLeft) {

	public long totalLeft() {
		return seatsLeft + sleepersLeft;
	}

	public boolean soldOut() {
		return totalLeft() == 0;
	}

	public long left(CoachType type) {
		return type == CoachType.SEAT ? seatsLeft : sleepersLeft;
	}

}
