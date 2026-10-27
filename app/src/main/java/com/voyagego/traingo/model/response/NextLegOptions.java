package com.voyagego.traingo.model.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What the trip wizard (templates/admin/trip-wizard.html) offers for a
 * train's next leg, from TrainScheduleService.nextLegOptions.
 *
 * @param chained true when the train has a trip that is not cancelled, so the
 *        next leg must start where that trip ends
 * @param origin where the next leg starts: the end of the chain, the station
 *        the admin picked for a train with no trip yet, or null before that
 * @param earliestDeparture the same moment the train page's banner shows
 * @param defaultDeparture the first 08:00 that is not before
 *        {@code earliestDeparture}
 * @param origins every station north to south, to pick the first origin from
 * @param destinations one per route that starts at {@code origin}
 * @param withoutRoute the other stations, which no route reaches from
 *        {@code origin}
 * @param lastTrips the last two trips of the schedule that are not cancelled
 */
public record NextLegOptions(long trainId, String trainCode, boolean chained, Origin origin,
		LocalDateTime earliestDeparture, LocalDateTime defaultDeparture, List<Origin> origins,
		List<Destination> destinations, List<Unrouted> withoutRoute, List<TripSummary> lastTrips) {

	public record Origin(long stationId, String name) {
	}

	/**
	 * A station the next leg can go to. {@code side} is "Vào Nam" when it lies
	 * south of the origin, else "Ra Bắc"; {@code minutes} and
	 * {@code suggestedPrice} are the suggestions for the route.
	 */
	public record Destination(long stationId, String name, String side, int distanceKm, int minutes,
			long suggestedPrice) {
	}

	/** A station with no route from the origin, and how far it is along the line. */
	public record Unrouted(long stationId, String name, int distanceKm) {
	}

	public record TripSummary(long tripId, LocalDateTime departureAt, LocalDateTime arrivalAt, String originName,
			String destinationName) {
	}

}
