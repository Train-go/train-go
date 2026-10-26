package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * What /admin/bookings shows, built inside the transaction by
 * BookingAdminService.list.
 *
 * @param tab the open group: upcoming, past or cancelled
 * @param q what the search box shows: the text typed, or a customer's name
 *        pre-filled when the page is opened for that customer
 * @param searching whether {@code q} was typed, so the list is really filtered by it
 * @param customerId the customer the list is limited to, or null for everyone
 * @param customerName that customer's name, which pre-fills the search box
 * @param upcomingCount bookings in each group, for the tab counters; they follow
 *        the customer filter but not the search text
 */
public record BookingListView(String tab, String q, boolean searching, Long customerId, String customerName, int upcomingCount,
		int pastCount, int cancelledCount, List<BookingRow> rows) {

	/** One line of the table; the labels are ready to print. */
	public record BookingRow(String code, String customerName, String customerEmail, String trainCode,
			String routeLabel, String departureLabel, int ticketCount, long totalPrice) {
	}

}
