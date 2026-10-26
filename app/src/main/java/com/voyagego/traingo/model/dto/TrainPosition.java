package com.voyagego.traingo.model.dto;

import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Trip;

/**
 * Where a train is at one moment, worked out from its trips that are not
 * cancelled (TrainScheduleService.positionOf):
 *
 * <ul>
 * <li>IDLE: the train has no such trip yet. Both other fields are null.</li>
 * <li>RUNNING: {@code trip} has departed and not yet arrived.</li>
 * <li>AT_STATION: the train stands at {@code station}, where its last trip
 * arrived, or where its first trip starts when none has run yet.</li>
 * </ul>
 */
public record TrainPosition(Kind kind, Trip trip, Station station) {

	public enum Kind {
		IDLE,
		RUNNING,
		AT_STATION
	}

	public static TrainPosition idle() {
		return new TrainPosition(Kind.IDLE, null, null);
	}

	public static TrainPosition running(Trip trip) {
		return new TrainPosition(Kind.RUNNING, trip, null);
	}

	public static TrainPosition atStation(Station station) {
		return new TrainPosition(Kind.AT_STATION, null, station);
	}

	/** One sentence for the banner of the train page. */
	public String description() {
		if (kind == Kind.RUNNING) {
			return "Đang chạy " + trip.getRoute().getOriginStation().getName() + " → "
					+ trip.getRoute().getDestinationStation().getName() + ".";
		}
		if (kind == Kind.AT_STATION) {
			return "Đang ở " + station.getName() + ".";
		}
		return "Chưa có chuyến nào.";
	}

}
