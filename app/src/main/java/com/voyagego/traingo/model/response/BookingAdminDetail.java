package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * One booking as the admin's detail sheet shows it. Served as JSON by
 * BookingAdminApiController, so the labels are ready to print.
 *
 * @param statusCss the .tag modifier of {@code statusLabel}: ok, running or cancelled
 * @param paidLabel when the booking was paid, or null while it is not paid
 * @param cancelBlockReason why the booking cannot be cancelled, or null when it can
 *        (a cancelled booking has no reason and no cancel button)
 */
public record BookingAdminDetail(String code, boolean cancelled, String statusLabel, String statusCss,
		String customerName, String customerEmail, String customerPhone, long tripId, String trainCode,
		String routeLabel, String departureLabel, long totalPrice, String paidLabel, List<TicketLine> tickets,
		String cancelBlockReason) {

	/** One passenger. {@code idMask} shows the last four characters of the identity number only. */
	public record TicketLine(String ticketCode, String fullName, String idMask, String seatLabel, long price) {
	}

}
