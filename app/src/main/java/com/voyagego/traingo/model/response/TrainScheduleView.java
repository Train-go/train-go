package com.voyagego.traingo.model.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.TrainPosition;

/**
 * Everything the "Theo ngày" view of /admin/trains/{id} shows, built inside
 * the transaction by TrainScheduleService.scheduleOf. The labels are ready to
 * print, so the template does no date arithmetic.
 *
 * @param chainEnd the last trip that is not cancelled, or null when the train
 *        has none; the next leg starts at its destination
 * @param scheduledUntil "T3 04/11 09:45", the arrival of {@code chainEnd}, or
 *        null without it
 * @param earliestDeparture when the next leg may depart at the earliest
 * @param earliestDepartureLabel {@code earliestDeparture} as "04/11 10:15"
 * @param pastCount how many trips have already arrived; the page hides them
 *        until the admin asks for them
 */
public record TrainScheduleView(Train train, TrainPosition position, Trip chainEnd, String scheduledUntil,
		LocalDateTime earliestDeparture, String earliestDepartureLabel, List<ScheduleDay> days, int pastCount) {

	public boolean hasTrips() {
		return !days.isEmpty();
	}

	/**
	 * One day of the schedule. {@code title} is "Hôm nay", "Ngày mai",
	 * "Hôm qua" or the weekday ("Thứ Năm"). {@code past} is true when every
	 * trip of the day has already run, so the whole day hides with them.
	 */
	public record ScheduleDay(LocalDate date, String title, String dateLabel, List<ScheduleItem> items,
			boolean past) {
	}

	/**
	 * One trip of the schedule.
	 *
	 * @param arrivalLabel "05:35", or "T5 22/10 05:35" when the trip arrives on
	 *        a later day than it departs
	 * @param phaseCss the .tag modifier of {@code phaseLabel}: scheduled,
	 *        running, done or cancelled
	 * @param past true once the trip has arrived; cancelled trips never are
	 * @param distanceKm length of the trip's route
	 * @param departureDayLabel "T5 22/10", for the spreadsheet view's date column
	 * @param arrivalPlusDays how many days after the departure day the trip
	 *        arrives, shown as "+1" next to the arrival time
	 */
	public record ScheduleItem(long tripId, LocalDateTime departureAt, LocalDateTime arrivalAt, String arrivalLabel,
			String originName, String destinationName, long basePrice, int bookedSeats, boolean cancelled,
			String phaseLabel, String phaseCss, boolean past, int distanceKm, String departureDayLabel,
			int arrivalPlusDays) {
	}

}
