package com.voyagego.traingo.model.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Bookings and revenue over a date range, grouped by day, week or month, as
 * JSON from GET /api/admin/overview/sales. Every bucket of the range has a
 * point; one without a sale reads zero.
 *
 * @param from  first day of the range, as the server settled it
 * @param to    last day, never after today
 * @param group "day", "week" or "month"
 */
public record SalesSeries(LocalDate from, LocalDate to, String group, List<Point> points, long bookings,
		long revenue) {

	/**
	 * @param date  first day of the bucket, ISO
	 * @param label what an axis prints: "28/10", or "10/2026" for a month
	 * @param title what a tooltip prints: "Tuần 26/10 - 01/11"
	 */
	public record Point(String date, String label, String title, long bookings, long revenue) {
	}

}
