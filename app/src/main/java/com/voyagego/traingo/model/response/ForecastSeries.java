package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * What is already booked for the trips of the next seven days, per day the
 * trips leave, as JSON from GET /api/admin/overview/forecast. It is money and
 * seats on the books, not a guess: the page reads it as the lower bound of the
 * week, which grows as customers keep booking.
 *
 * @param revenue  total of the points, in dong
 * @param sold     total seats sold
 * @param capacity total seats on the trips of the week
 */
public record ForecastSeries(List<Point> points, long revenue, long sold, long capacity) {

	/**
	 * @param label       "T4 29/10"
	 * @param fillPercent seats sold out of seats on the day's trips, 0 to 100
	 */
	public record Point(String date, String label, long trips, long sold, long capacity, int fillPercent,
			long revenue) {
	}

}
