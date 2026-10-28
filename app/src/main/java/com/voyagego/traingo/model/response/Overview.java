package com.voyagego.traingo.model.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the admin overview page prints (#37): the four totals with their
 * 7-day trend, what needs attention, and the week ahead of each train. The two
 * charts are drawn from {@link SalesSeries} and {@link ForecastSeries}.
 *
 * @param bookings          CONFIRMED bookings
 * @param cancelledBookings CANCELLED bookings, the note under "Booking"
 * @param trips             trips that are not cancelled
 * @param upcomingTrips     SCHEDULED trips that have not left, the note under "Chuyến"
 * @param customers         users with the CUSTOMER role
 * @param revenue           total_price of CONFIRMED bookings, in dong
 * @param bookingTrend      bookings paid in the last 7 days against the 7 days before
 * @param revenueTrend      the same for revenue, in dong
 * @param alerts            the most urgent first, at most {@link #MAX_ALERTS}
 * @param alertTotal        how many alerts there are, shown ones included
 * @param week              the seven days from today, the columns of the train grid
 * @param trains            the trains that need a look first, at most {@link #MAX_TRAINS}
 * @param trainTotal        how many trains there are, shown ones included
 */
public record Overview(long bookings, long cancelledBookings, long trips, long upcomingTrips, long customers,
		long revenue, Trend bookingTrend, Trend revenueTrend, List<Alert> alerts, int alertTotal,
		List<WeekDay> week, List<TrainWeek> trains, int trainTotal) {

	public static final int MAX_ALERTS = 8;
	public static final int MAX_TRAINS = 8;

	/** A figure over the last 7 days and over the 7 days before them. */
	public record Trend(long current, long previous) {

		public long delta() {
			return current - previous;
		}

	}

	/**
	 * @param level  "danger", "warning" or "info"
	 * @param icon   a Bootstrap icon name
	 * @param href   where to act on it, or null
	 */
	public record Alert(String level, String icon, String title, String detail, String href) {
	}

	/** @param label "T4", "CN" ... */
	public record WeekDay(LocalDate date, String label, String dayMonth, boolean today) {
	}

	/**
	 * One train's week: trips on each of the seven days, in the order of
	 * {@code week}. {@code level} is the worst alert of the train, or null.
	 */
	public record TrainWeek(long id, String code, String name, List<Integer> trips, String level) {
	}

}
