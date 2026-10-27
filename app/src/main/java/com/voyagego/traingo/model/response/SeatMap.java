package com.voyagego.traingo.model.response;

import java.time.LocalDateTime;
import java.util.List;

import com.voyagego.traingo.entity.CoachType;

/**
 * The seat page's data for one trip, as JSON from GET /api/trips/{id}/seat-map.
 * Every seat on a trip costs the same, {@code trips.base_price}, so the price
 * sits in the trip summary and not on a coach.
 *
 * @param coaches in coach number order, each with its seats in seat-code order
 */
public record SeatMap(TripSummary trip, List<CoachMap> coaches) {

	/**
	 * @param durationLabel  running time as "1g45", or "8g" on the hour
	 * @param arrivesNextDay days between the departure date and the arrival date
	 */
	public record TripSummary(long tripId, String trainCode, String originCode, String originName,
			String destinationCode, String destinationName, LocalDateTime departureAt, LocalDateTime arrivalAt,
			String durationLabel, int arrivesNextDay, long price) {
	}

	/**
	 * @param label "Ghế ngồi" or "Giường nằm"
	 * @param available seats still free when the map was read
	 */
	public record CoachMap(int number, CoachType type, String label, int available, int capacity,
			List<SeatCell> seats) {
	}

	/** @param cabin 1 to 4 for a bed, null for a seat */
	public record SeatCell(long id, String code, Integer cabin, boolean available) {
	}

}
