package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * What /admin/bookings shows, built inside the transaction by
 * BookingAdminService.list: bookings under the trip they travel on, paged by
 * trip.
 *
 * @param tab the open group: upcoming, past or cancelled
 * @param q the text typed in the search box
 * @param searching whether the list is narrowed by the text or a customer
 * @param customerId the customer the list is limited to, or null for everyone
 * @param customerName that customer's name
 * @param limit how many trips are shown at most
 * @param tripTotal how many trips the group and the search hold
 * @param upcomingCount bookings in each group, for the tab counters; they follow
 *        the customer filter but not the search text
 */
public record BookingListView(String tab, String q, boolean searching, Long customerId, String customerName, int limit,
		int tripTotal, int upcomingCount, int pastCount, int cancelledCount, List<TripGroup> trips) {

	/** How many trips "Hiện thêm" would add. */
	public int more() {
		return Math.min(10, tripTotal - trips.size());
	}

	/**
	 * One trip and its bookings; the labels are ready to print.
	 *
	 * @param dayLabel "Hôm nay", a weekday...; null when the trip above left on the same day
	 * @param coaches the fill per coach, or null when a bar would mean nothing
	 */
	public record TripGroup(long tripId, String dayLabel, String dayDate, String time, String trainCode, String route,
			String summary, List<CoachFill> coaches, List<BookingLine> bookings) {
	}

	public record BookingLine(String code, String customerName, String customerEmail, long totalPrice, boolean cancelled,
			List<Passenger> passengers) {
	}

	/** {@code seat} is "Toa 1, A05", or "Đã trả chỗ" on a cancelled booking. */
	public record Passenger(String name, String seat) {
	}

}
