package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * One of the customer's bookings at /my-trips/{id}. Identity numbers are
 * masked before they reach the page.
 *
 * @param departureLabel "Thứ Năm, 29/10/2026 · 08:00"
 * @param arrivalLabel "14:50", or "06:10 30/10" when the trip arrives on a later day
 * @param paidLabel "26/10/2026 14:05", or null when not paid
 * @param cancellable true when the "Hủy booking" button shows
 * @param cancelQuestion the body of the cancel dialog, null when not cancellable
 */
public record BookingDetail(long id, String code, String routeLabel, String departureLabel, String arrivalLabel,
		String trainLabel, List<TicketLine> tickets, long totalPrice, String paidLabel, String statusLabel,
		String statusCss, boolean cancellable, String cancelQuestion) {

	/** @param seatLabel "Toa 3 · B02" */
	public record TicketLine(long id, String ticketCode, String fullName, String idMask, String seatLabel, long price) {
	}

}
