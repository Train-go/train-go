package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * Bookings and revenue per day over the last {@code days} days, today
 * included, as JSON from GET /api/admin/overview/sales. Every day has a point,
 * a day without a sale reads zero.
 */
public record SalesSeries(int days, List<Point> points, long bookings, long revenue) {

	/** @param label "29/10" */
	public record Point(String date, String label, long bookings, long revenue) {
	}

}
