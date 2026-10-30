package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * The "Theo ngày" view of /admin/trains/{id}: four facts on where the train
 * stands, then a row per day with its legs placed on the 24 hours. Positions
 * are percentages of the day, ready for the CSS variables; every label is ready
 * to print. Built by TrainScheduleService.
 *
 * @param nowPercent where "now" sits on the axis, or null when today has no row
 * @param nowLabel the clock on that marker
 */
public record TrainHoursView(List<Fact> facts, List<HourRow> rows, String nowPercent, String nowLabel) {

	/**
	 * @param meter a percentage to draw under the value, or null
	 * @param running paints the value in the running colour
	 */
	public record Fact(String label, String value, String sub, String sub2, Integer meter, boolean running) {
	}

	/**
	 * One day.
	 *
	 * @param past the day has only legs that have run, so it hides with them
	 * @param nowPercent the marker of "now" when this row is today, else null
	 * @param slotPercent where the next leg may start when that falls on this day, else null
	 * @param nightText where the train is when the day ends, blank when unknown
	 * @param nightRunning the train is on a leg at that moment
	 * @param alert the sentence on a chain cut by a cancelled trip, or null
	 */
	public record HourRow(String title, String dateLabel, boolean today, boolean past, List<HourLeg> legs,
			List<HourWait> waits, String nowPercent, String slotPercent, String slotLabel, String slotAria,
			String nightText, boolean nightRunning, String alert) {
	}

	/**
	 * A leg, or the part of a leg that falls on this day.
	 *
	 * @param css the leg's classes: is-up, is-run, is-done or is-off, then is-cont,
	 *        is-on and is-break when they apply
	 * @param past hidden until the admin asks for legs that have run
	 * @param room how far the words may run, up to the next start of the day
	 * @param fill the share of seats sold, or null on a cancelled leg
	 * @param continued the leg started the day before, so the words name its arrival
	 */
	public record HourLeg(long tripId, String css, boolean past, String x, String width, String room, Integer fill,
			String aria, boolean continued, String time, String longText, String shortText) {
	}

	public record HourWait(String x, String text) {
	}

}
