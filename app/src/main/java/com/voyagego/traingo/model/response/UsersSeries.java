package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * Customers registered per day over the last {@code days} days, today
 * included, as JSON from GET /api/admin/overview/users. Every day has a
 * point; {@code total} on a point is how many customers exist at the end of
 * that day.
 *
 * @param newUsers customers registered in the whole range
 * @param total    customers today
 */
public record UsersSeries(int days, List<Point> points, long newUsers, long total) {

	/** @param label "29/10" */
	public record Point(String date, String label, long newUsers, long total) {
	}

}
