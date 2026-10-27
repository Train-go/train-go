package com.voyagego.traingo.model.response;

import java.util.List;

import com.voyagego.traingo.model.response.SeatMap.TripSummary;

/**
 * The bill on the payment page: the trip, one line per ticket and the total.
 * Built from the booking draft, so nothing here exists in the database yet.
 */
public record Invoice(TripSummary trip, List<Line> lines, long total) {

	/** @param typeLabel "Ghế ngồi" or "Giường nằm" */
	public record Line(String seatCode, int coachNumber, String typeLabel, String passengerName, long price) {
	}

}
