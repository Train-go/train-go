package com.voyagego.traingo.model.response;

import java.util.List;

import com.voyagego.traingo.model.response.SeatMap.TripSummary;

/**
 * What the passenger page prints around the form: the trip, one slot per
 * chosen seat, the total and the account holder's name and masked phone. The
 * names and CCCD being edited live in the form, not here.
 *
 * @param accountName the signed-in customer's name, the default passenger
 * @param maskedPhone "•••• 4567", or null when the account has no phone
 */
public record PassengerPage(TripSummary trip, List<Slot> slots, long total, String accountName,
		String maskedPhone) {

	/** @param typeLabel "Ghế ngồi" or "Giường nằm" */
	public record Slot(int number, long seatId, int coachNumber, String seatCode, String typeLabel, long price) {
	}

}
