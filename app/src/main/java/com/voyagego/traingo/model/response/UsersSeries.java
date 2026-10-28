package com.voyagego.traingo.model.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Customers registered over a date range, grouped by day, week or month, as
 * JSON from GET /api/admin/overview/users. {@code total} on a point is how
 * many customers exist at the end of that bucket.
 *
 * @param newUsers customers registered in the whole range
 * @param total    customers at the end of the range
 */
public record UsersSeries(LocalDate from, LocalDate to, String group, List<Point> points, long newUsers,
		long total) {

	/**
	 * @param label what an axis prints
	 * @param title what a tooltip prints
	 */
	public record Point(String date, String label, String title, long newUsers, long total) {
	}

}
