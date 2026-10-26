package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * One trip as the admin's trip sheet shows it: the facts of the trip, its
 * four coaches with every seat, and the passengers of confirmed bookings.
 * Served as JSON by TripAdminApiController, so the labels are ready to print.
 *
 * @param statusCss the .tag modifier of {@code statusLabel}: scheduled, running, done or cancelled
 * @param departureLabel "T5 22/10 08:00"
 * @param arrivalLabel "10:35", or "T6 23/10 02:20" when it arrives on a later day
 * @param bookedCount seats that are sold
 */
public record TripDetail(long id, long trainId, String trainCode, String trainName, String routeLabel,
		String statusLabel, String statusCss, String departureLabel, String arrivalLabel, int distanceKm,
		long basePrice, int bookedCount, int totalSeats, List<CoachView> coaches, List<PassengerRow> passengers) {

	/** {@code typeLabel} is "Ghế ngồi" or "Giường nằm"; {@code sleeper} tells the map to group beds by cabin. */
	public record CoachView(int number, boolean sleeper, String typeLabel, int capacity, int sold,
			List<SeatView> seats) {
	}

	/** {@code cabin} is null for seats; {@code passengerName} and {@code bookingCode} are null while the seat is free. */
	public record SeatView(String code, Integer cabin, boolean sold, String passengerName, String bookingCode) {
	}

	/** {@code idMask} shows the last four characters of the identity number only. */
	public record PassengerRow(String fullName, String idMask, int coachNumber, String seatCode, String bookingCode,
			long price) {
	}

}
