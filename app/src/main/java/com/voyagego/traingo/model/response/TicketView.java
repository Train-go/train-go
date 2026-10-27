package com.voyagego.traingo.model.response;

import java.time.LocalDateTime;

/**
 * One e-ticket as the customer sees it at /tickets/{id}. The identity number
 * arrives masked to its last four digits (maskedId, "•••• 1234").
 *
 * @param durationLabel  running time as "1g45", or "8g" on the hour
 * @param arrivesNextDay days between the departure date and the arrival date
 * @param coachTypeLabel "Ghế ngồi" or "Giường nằm"
 * @param cancelled      the booking was cancelled, so the ticket is void
 */
public record TicketView(long id, long bookingId, String ticketCode, String bookingCode, String passengerName,
		String maskedId, String trainCode, String trainName, String originName, String destinationName,
		LocalDateTime departureAt, LocalDateTime arrivalAt, String durationLabel, int arrivesNextDay,
		int coachNumber, String seatCode, String coachTypeLabel, long price, boolean cancelled) {
}
