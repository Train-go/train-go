package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.TrainNotFoundException;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.dto.TrainPosition;
import com.voyagego.traingo.model.response.TrainScheduleView;
import com.voyagego.traingo.model.response.TrainScheduleView.ScheduleDay;
import com.voyagego.traingo.model.response.TrainScheduleView.ScheduleItem;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * A train's schedule and the leg-chaining rules of
 * docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md: the schedule is the
 * train's trips that are not cancelled, ordered by departure, and a new trip
 * may only be added after the last one.
 *
 * The trip-creation tasks add their checks here, next to the rules they rely
 * on; they load the train through TrainService.findById, which refuses a
 * soft-deleted train. Every method takes {@code now} from its caller, so one
 * page shows one moment.
 */
@Service
@Transactional(readOnly = true)
public class TrainScheduleService {

	/** Time a train needs at a station before its next leg. */
	public static final Duration TURNAROUND = Duration.ofMinutes(30);

	private static final int ROUND_TO_MINUTES = 5;
	private static final String[] WEEKDAYS = { "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy",
			"Chủ nhật" };
	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final TrainRepository trainRepository;
	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;

	public TrainScheduleService(TrainRepository trainRepository, TripRepository tripRepository,
			TripSeatRepository tripSeatRepository) {
		this.trainRepository = trainRepository;
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
	}

	/**
	 * The trip with the latest departure among those not cancelled. The next
	 * leg starts at its destination. Empty when the train has no such trip, and
	 * then the next leg may start anywhere.
	 */
	public Optional<Trip> chainEnd(long trainId) {
		return tripRepository.findFirstByTrainIdAndStatusNotOrderByDepartureAtDesc(trainId, TripStatus.CANCELLED);
	}

	/**
	 * The arrival of the last trip plus {@link #TURNAROUND}. A departure must
	 * also lie in the future, so when that moment has passed, or the train has
	 * no trip yet, it is {@code now} rounded up to the next 5 minutes.
	 */
	public LocalDateTime earliestDeparture(long trainId, LocalDateTime now) {
		return earliestDeparture(chainEnd(trainId).orElse(null), now);
	}

	public TrainPosition positionOf(long trainId, LocalDateTime now) {
		return positionOf(tripRepository.findScheduleOfTrain(trainId), now);
	}

	public TrainScheduleView scheduleOf(long trainId, LocalDateTime now) {
		// A deleted train has no page, the same as an unknown id.
		Train train = trainRepository.findByIdAndDeletedFalse(trainId).orElseThrow(TrainNotFoundException::new);
		List<Trip> trips = tripRepository.findScheduleOfTrain(trainId);
		Trip end = chainEnd(trainId).orElse(null);
		LocalDateTime earliest = earliestDeparture(end, now);

		Map<Long, Integer> bookedByTrip = new HashMap<>();
		for (IdCount count : tripSeatRepository.countBookedByTrain(trainId)) {
			bookedByTrip.put(count.getId(), (int) count.getTotal());
		}

		// The trips come ordered by departure, so a new day starts whenever the
		// departure date changes.
		List<ScheduleDay> days = new ArrayList<>();
		List<ScheduleItem> items = new ArrayList<>();
		LocalDate day = null;
		int pastCount = 0;
		for (Trip trip : trips) {
			LocalDate tripDay = trip.getDepartureAt().toLocalDate();
			if (day != null && !tripDay.equals(day)) {
				days.add(toDay(day, items, now));
				items = new ArrayList<>();
			}
			day = tripDay;
			ScheduleItem item = toItem(trip, bookedByTrip.getOrDefault(trip.getId(), 0), now);
			if (item.past()) {
				pastCount++;
			}
			items.add(item);
		}
		if (day != null) {
			days.add(toDay(day, items, now));
		}

		String scheduledUntil = end == null ? null : dayLabel(end.getArrivalAt()) + " " + end.getArrivalAt().format(TIME);
		String earliestLabel = earliest.format(DAY_MONTH) + " " + earliest.format(TIME);
		return new TrainScheduleView(train, positionOf(trips, now), end, scheduledUntil, earliest, earliestLabel, days,
				pastCount);
	}

	private static LocalDateTime earliestDeparture(Trip end, LocalDateTime now) {
		if (end != null) {
			LocalDateTime afterTurnaround = end.getArrivalAt().plus(TURNAROUND);
			if (afterTurnaround.isAfter(now)) {
				return afterTurnaround;
			}
		}
		return roundUp(now);
	}

	/** 10:02:40 becomes 10:05, 10:05:30 stays 10:05. */
	private static LocalDateTime roundUp(LocalDateTime time) {
		LocalDateTime minute = time.truncatedTo(ChronoUnit.MINUTES);
		int extra = minute.getMinute() % ROUND_TO_MINUTES;
		if (extra == 0) {
			return minute;
		}
		return minute.plusMinutes(ROUND_TO_MINUTES - extra);
	}

	/** {@code trips} ordered by departure, cancelled ones included. */
	private static TrainPosition positionOf(List<Trip> trips, LocalDateTime now) {
		Trip first = null;
		Trip lastArrived = null;
		for (Trip trip : trips) {
			if (trip.isCancelled()) {
				continue;
			}
			if (trip.isRunning(now)) {
				return TrainPosition.running(trip);
			}
			if (first == null) {
				first = trip;
			}
			if (!trip.getArrivalAt().isAfter(now)) {
				lastArrived = trip;
			}
		}
		if (lastArrived != null) {
			return TrainPosition.atStation(lastArrived.getRoute().getDestinationStation());
		}
		if (first != null) {
			return TrainPosition.atStation(first.getRoute().getOriginStation());
		}
		return TrainPosition.idle();
	}

	private static ScheduleDay toDay(LocalDate date, List<ScheduleItem> items, LocalDateTime now) {
		boolean allPast = true;
		for (ScheduleItem item : items) {
			if (!item.past()) {
				allPast = false;
			}
		}
		return new ScheduleDay(date, dayTitle(date, now.toLocalDate()), date.format(DATE), items, allPast);
	}

	private static ScheduleItem toItem(Trip trip, int bookedSeats, LocalDateTime now) {
		LocalDateTime departure = trip.getDepartureAt();
		LocalDateTime arrival = trip.getArrivalAt();
		String arrivalLabel = arrival.format(TIME);
		if (!arrival.toLocalDate().equals(departure.toLocalDate())) {
			arrivalLabel = dayLabel(arrival) + " " + arrivalLabel;
		}
		boolean past = !trip.isCancelled() && !arrival.isAfter(now);
		return new ScheduleItem(trip.getId(), departure, arrival, arrivalLabel,
				trip.getRoute().getOriginStation().getName(), trip.getRoute().getDestinationStation().getName(),
				trip.getBasePrice(), bookedSeats, trip.isCancelled(), trip.phaseLabel(now), phaseCss(trip, now), past);
	}

	/** The .tag modifiers in css/admin/admin.css. */
	private static String phaseCss(Trip trip, LocalDateTime now) {
		if (trip.isCancelled()) {
			return "cancelled";
		}
		if (!trip.hasDeparted(now)) {
			return "scheduled";
		}
		if (trip.isRunning(now)) {
			return "running";
		}
		return "done";
	}

	private static String dayTitle(LocalDate date, LocalDate today) {
		if (date.equals(today)) {
			return "Hôm nay";
		}
		if (date.equals(today.plusDays(1))) {
			return "Ngày mai";
		}
		if (date.equals(today.minusDays(1))) {
			return "Hôm qua";
		}
		return WEEKDAYS[date.getDayOfWeek().getValue() - 1];
	}

	/** "T5 22/10". */
	private static String dayLabel(LocalDateTime time) {
		return SHORT_WEEKDAYS[time.getDayOfWeek().getValue() - 1] + " " + time.format(DAY_MONTH);
	}

}
