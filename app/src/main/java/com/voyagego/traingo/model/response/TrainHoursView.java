package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * The "Theo ngày" view of /admin/trains/{id}: four facts on where the train
 * stands, and the trips for the calendar. The calendar half is also served as
 * JSON by GET /api/admin/trains/{id}/timeline for static/js/admin/train-timeline.js
 * (FullCalendar). Every label is ready to print. Built by TrainScheduleService.
 */
public record TrainHoursView(List<Fact> facts, Timeline timeline) {

	/**
	 * @param meter a percentage to draw under the value, or null
	 * @param running paints the value in the running colour
	 */
	public record Fact(String label, String value, String sub, String sub2, Integer meter, boolean running) {
	}

	/**
	 * What the calendar draws.
	 *
	 * @param legs every trip of the train, cancelled ones included, by departure
	 * @param slot where a new trip may start at the earliest, or null when the train has no trip to follow
	 * @param alerts one sentence for each chain cut by a cancelled trip
	 */
	public record Timeline(List<Leg> legs, Slot slot, List<String> alerts) {
	}

	/**
	 * A trip as a block on the calendar.
	 *
	 * @param start departure, ISO local date-time
	 * @param end arrival, ISO local date-time
	 * @param css is-up, is-run, is-done or is-off, then is-break when the chain is cut before it
	 * @param past the trip has arrived and is not cancelled, so the calendar hides it until asked
	 * @param time the departure clock, "08:00"
	 * @param until the arrival clock, "10:55"
	 * @param route "Ga Hà Nội → Ga Huế"
	 * @param sold "Đã bán 12/96", or null on a cancelled trip
	 * @param fill the share of seats sold, or null on a cancelled trip
	 * @param aria the whole trip in one sentence, also the hover title
	 */
	public record Leg(long tripId, String start, String end, String css, boolean past, boolean cancelled, String time,
			String until, String route, String sold, Integer fill, String aria) {
	}

	/**
	 * @param at the earliest departure of a new trip, ISO local date-time
	 * @param label "Chuyến mới 15:50"
	 */
	public record Slot(String at, String label, String aria) {
	}

}
