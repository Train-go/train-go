package com.voyagego.traingo.model.response;

import java.time.LocalDateTime;

import com.voyagego.traingo.entity.Route;

/**
 * One leg of a multi-leg plan from TrainScheduleService.planLegs: times and
 * price are the suggestions for that route, which the admin may still change.
 */
public record PlannedLeg(long originStationId, long destinationStationId, String originName,
		String destinationName, int distanceKm, LocalDateTime departureAt, LocalDateTime arrivalAt,
		long basePrice) {

	public static PlannedLeg of(Route route, LocalDateTime departureAt, LocalDateTime arrivalAt, long basePrice) {
		return new PlannedLeg(route.getOriginStation().getId(), route.getDestinationStation().getId(),
				route.getOriginStation().getName(), route.getDestinationStation().getName(), route.getDistanceKm(),
				departureAt, arrivalAt, basePrice);
	}

}
