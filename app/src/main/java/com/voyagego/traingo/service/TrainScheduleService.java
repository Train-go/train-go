package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.LegPlanException;
import com.voyagego.traingo.common.exception.TrainNotFoundException;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.dto.TrainPosition;
import com.voyagego.traingo.model.response.PlannedLeg;
import com.voyagego.traingo.model.response.TrainScheduleView;
import com.voyagego.traingo.model.response.TrainScheduleView.ScheduleDay;
import com.voyagego.traingo.model.response.TrainScheduleView.ScheduleItem;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * A train's schedule and the leg-chaining rules of
 * docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md: the schedule is the
 * train's trips that are not cancelled, ordered by departure, and a new trip
 * may only be added after the last one.
 *
 * TripCreationService adds trips under these rules; it loads the train through
 * TrainRepository.findByIdForUpdate, which refuses a soft-deleted train and
 * locks the row. Every method takes {@code now} from its caller, so one page
 * shows one moment.
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

	private static final LocalTime DAILY_DEPARTURE = LocalTime.of(8, 0);

	/** How the legs of a plan follow each other. */
	public enum Pace {
		/** One leg a day: each leg leaves at the first 08:00 after the turnaround. */
		DAILY,
		/** Each leg leaves as soon as the turnaround is over. */
		NONSTOP
	}

	private final TrainRepository trainRepository;
	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;
	private final StationRepository stationRepository;
	private final RouteRepository routeRepository;

	public TrainScheduleService(TrainRepository trainRepository, TripRepository tripRepository,
			TripSeatRepository tripSeatRepository, StationRepository stationRepository,
			RouteRepository routeRepository) {
		this.trainRepository = trainRepository;
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
		this.stationRepository = stationRepository;
		this.routeRepository = routeRepository;
	}

	/** Running time of a route: 60 km/h, rounded to 5 minutes, at least 5. */
	public static int suggestedMinutes(int km) {
		return (int) Math.max(5, Math.round(km / 5.0) * 5);
	}

	/** Price of a route: about 1.000đ per km, rounded to 10.000đ, at least 10.000đ. */
	public static long suggestedPrice(int km) {
		return Math.max(10_000, Math.round(km * 1000 / 10_000.0) * 10_000);
	}

	/**
	 * The routes from one station to another, one per pair of neighbouring
	 * stations along the line (ordered by km mark), in travel order. Empty when
	 * a station is unknown, the two are the same, or any pair on the way has no
	 * route in that direction.
	 */
	public Optional<List<Route>> adjacentPath(long fromStationId, long toStationId) {
		List<Station> line = stationRepository.findAllByOrderByKmMarkAsc();
		int from = indexOf(line, fromStationId);
		int to = indexOf(line, toStationId);
		if (from < 0 || to < 0 || from == to) {
			return Optional.empty();
		}
		Map<List<Long>, Route> routes = new HashMap<>();
		for (Route route : routeRepository.findAllWithStations()) {
			routes.put(List.of(route.getOriginStation().getId(), route.getDestinationStation().getId()), route);
		}
		int step = to > from ? 1 : -1;
		List<Route> path = new ArrayList<>();
		for (int i = from; i != to; i += step) {
			Route route = routes.get(List.of(line.get(i).getId(), line.get(i + step).getId()));
			if (route == null) {
				return Optional.empty();
			}
			path.add(route);
		}
		return Optional.of(path);
	}

	/**
	 * One leg per route of {@link #adjacentPath}, each with the suggested
	 * running time and price. The first leaves at {@code firstDeparture}; the
	 * next one after the turnaround, or at 08:00 for {@link Pace#DAILY}.
	 */
	public List<PlannedLeg> planLegs(long trainId, long fromStationId, long toStationId, Pace pace,
			LocalDateTime firstDeparture) {
		// A deleted train has no page, the same as an unknown id.
		trainRepository.findByIdAndDeletedFalse(trainId).orElseThrow(TrainNotFoundException::new);
		if (fromStationId == toStationId) {
			throw LegPlanException.sameStation();
		}
		List<Route> path = adjacentPath(fromStationId, toStationId).orElseThrow(LegPlanException::missingRoute);
		List<PlannedLeg> legs = new ArrayList<>();
		LocalDateTime departure = firstDeparture;
		for (Route route : path) {
			LocalDateTime arrival = departure.plusMinutes(suggestedMinutes(route.getDistanceKm()));
			legs.add(PlannedLeg.of(route, departure, arrival, suggestedPrice(route.getDistanceKm())));
			LocalDateTime ready = arrival.plus(TURNAROUND);
			departure = pace == Pace.DAILY ? firstDailyDeparture(ready) : ready;
		}
		return legs;
	}

	private static int indexOf(List<Station> line, long stationId) {
		for (int i = 0; i < line.size(); i++) {
			if (line.get(i).getId() == stationId) {
				return i;
			}
		}
		return -1;
	}

	/** The first 08:00 that is not before {@code time}. */
	private static LocalDateTime firstDailyDeparture(LocalDateTime time) {
		LocalDateTime sameDay = time.toLocalDate().atTime(DAILY_DEPARTURE);
		return sameDay.isBefore(time) ? sameDay.plusDays(1) : sameDay;
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
		int plusDays = (int) ChronoUnit.DAYS.between(departure.toLocalDate(), arrival.toLocalDate());
		return new ScheduleItem(trip.getId(), departure, arrival, arrivalLabel,
				trip.getRoute().getOriginStation().getName(), trip.getRoute().getDestinationStation().getName(),
				trip.getBasePrice(), bookedSeats, trip.isCancelled(), trip.phaseLabel(now), phaseCss(trip, now), past,
				trip.getRoute().getDistanceKm(), dayLabel(departure), plusDays);
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
