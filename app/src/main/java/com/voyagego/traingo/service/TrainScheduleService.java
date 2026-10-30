package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.LegPlanException;
import com.voyagego.traingo.common.exception.StationNotFoundException;
import com.voyagego.traingo.common.exception.TrainNotFoundException;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.dto.TrainPosition;
import com.voyagego.traingo.model.response.NextLegOptions;
import com.voyagego.traingo.model.response.PlannedLeg;
import com.voyagego.traingo.model.response.TrainHoursView;
import com.voyagego.traingo.model.response.TrainHoursView.Fact;
import com.voyagego.traingo.model.response.TrainHoursView.HourLeg;
import com.voyagego.traingo.model.response.TrainHoursView.HourRow;
import com.voyagego.traingo.model.response.TrainHoursView.HourWait;
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
			departure = pace == Pace.DAILY ? defaultDeparture(ready) : ready;
		}
		return legs;
	}

	/**
	 * The stations a plan from {@code origin} can end at: walking away from the
	 * origin along the line, each one up to the first pair of neighbours with no
	 * route in the travel direction, both ways.
	 */
	private List<Long> chainReach(List<Station> line, Station origin) {
		Set<List<Long>> routes = new HashSet<>();
		for (Route route : routeRepository.findAllWithStations()) {
			routes.add(List.of(route.getOriginStation().getId(), route.getDestinationStation().getId()));
		}
		int at = indexOf(line, origin.getId());
		List<Long> reach = new ArrayList<>();
		for (int step : new int[] { -1, 1 }) {
			for (int i = at; i + step >= 0 && i + step < line.size()
					&& routes.contains(List.of(line.get(i).getId(), line.get(i + step).getId())); i += step) {
				reach.add(line.get(i + step).getId());
			}
		}
		return reach;
	}

	private static int indexOf(List<Station> line, long stationId) {
		for (int i = 0; i < line.size(); i++) {
			if (line.get(i).getId() == stationId) {
				return i;
			}
		}
		return -1;
	}

	/** The first 08:00 that is not before {@code time}: where a new leg's departure starts. */
	public static LocalDateTime defaultDeparture(LocalDateTime time) {
		LocalDateTime sameDay = time.toLocalDate().atTime(DAILY_DEPARTURE);
		return sameDay.isBefore(time) ? sameDay.plusDays(1) : sameDay;
	}

	/**
	 * The choices of the trip wizard's next leg. A train with trips continues
	 * from the end of its chain, and {@code originId} is ignored; a train with
	 * none starts at {@code originId}, and until one is picked the options only
	 * list the stations to start from.
	 */
	public NextLegOptions nextLegOptions(long trainId, Long originId, LocalDateTime now) {
		// A deleted train has no page, the same as an unknown id.
		Train train = trainRepository.findByIdAndDeletedFalse(trainId).orElseThrow(TrainNotFoundException::new);
		Trip end = chainEnd(trainId).orElse(null);
		LocalDateTime earliest = earliestDeparture(end, now);
		List<Station> line = stationRepository.findAllByOrderByKmMarkAsc();

		Station origin = null;
		if (end != null) {
			origin = end.getRoute().getDestinationStation();
		} else if (originId != null) {
			origin = line.stream().filter(station -> station.getId().equals(originId)).findFirst()
					.orElseThrow(StationNotFoundException::new);
		}

		List<NextLegOptions.Destination> destinations = new ArrayList<>();
		List<NextLegOptions.Unrouted> withoutRoute = new ArrayList<>();
		List<Long> chainReach = new ArrayList<>();
		if (origin != null) {
			chainReach = chainReach(line, origin);
			Set<Long> reached = new HashSet<>();
			for (Route route : routeRepository.findFromStation(origin.getId())) {
				Station to = route.getDestinationStation();
				int km = route.getDistanceKm();
				reached.add(to.getId());
				destinations.add(new NextLegOptions.Destination(to.getId(), to.getName(),
						to.getKmMark() > origin.getKmMark() ? "Vào Nam" : "Ra Bắc", km, suggestedMinutes(km),
						suggestedPrice(km)));
			}
			for (Station station : line) {
				if (!station.getId().equals(origin.getId()) && !reached.contains(station.getId())) {
					withoutRoute.add(new NextLegOptions.Unrouted(station.getId(), station.getName(),
							Math.abs(station.getKmMark() - origin.getKmMark())));
				}
			}
		}

		List<Trip> live = tripRepository.findScheduleOfTrain(trainId).stream().filter(trip -> !trip.isCancelled())
				.toList();
		List<NextLegOptions.TripSummary> lastTrips = live.subList(Math.max(0, live.size() - 2), live.size()).stream()
				.map(trip -> new NextLegOptions.TripSummary(trip.getId(), trip.getDepartureAt(), trip.getArrivalAt(),
						trip.getRoute().getOriginStation().getName(),
						trip.getRoute().getDestinationStation().getName()))
				.toList();

		return new NextLegOptions(train.getId(), train.getCode(), end != null,
				origin == null ? null : new NextLegOptions.Origin(origin.getId(), origin.getName()), earliest,
				defaultDeparture(earliest),
				line.stream().map(station -> new NextLegOptions.Origin(station.getId(), station.getName())).toList(),
				destinations, withoutRoute, chainReach, lastTrips);
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
				pastCount, hours(trips, end, earliest, bookedByTrip, now));
	}

	/** {@code trips} ordered by departure, cancelled ones included. */
	private static TrainHoursView hours(List<Trip> trips, Trip end, LocalDateTime earliest, Map<Long, Integer> booked,
			LocalDateTime now) {
		List<Trip> live = trips.stream().filter(trip -> !trip.isCancelled()).toList();
		List<HourRow> rows = new ArrayList<>();
		LocalDate today = now.toLocalDate();
		if (!trips.isEmpty()) {
			Map<Long, Trip> broken = chainBreaks(trips, live);
			LocalDate first = trips.get(0).getDepartureAt().toLocalDate();
			LocalDate start = first.isBefore(today) ? first : today;
			LocalDate last = earliest.toLocalDate().isAfter(today) ? earliest.toLocalDate() : today;
			for (Trip trip : live) {
				if (trip.getArrivalAt().toLocalDate().isAfter(last)) {
					last = trip.getArrivalAt().toLocalDate();
				}
			}
			for (Trip trip : trips) {
				if (trip.getDepartureAt().toLocalDate().isAfter(last)) {
					last = trip.getDepartureAt().toLocalDate();
				}
			}
			for (LocalDate day = start; !day.isAfter(last); day = day.plusDays(1)) {
				rows.add(hourRow(day, trips, live, broken, end, earliest, booked, now));
			}
		}
		boolean hasToday = rows.stream().anyMatch(HourRow::today);
		return new TrainHoursView(facts(live, end, earliest, booked, now), rows,
				hasToday ? percent(now.toLocalTime().toSecondOfDay() * 1000L) : null, now.format(TIME));
	}

	/** Where the chain of legs breaks because a trip in between was cancelled: next leg to the cancelled trip. */
	private static Map<Long, Trip> chainBreaks(List<Trip> trips, List<Trip> live) {
		Map<Long, Trip> found = new HashMap<>();
		for (int i = 0; i + 1 < live.size(); i++) {
			Trip leg = live.get(i);
			Trip next = live.get(i + 1);
			if (next.getRoute().getOriginStation().getId().equals(leg.getRoute().getDestinationStation().getId())) {
				continue;
			}
			trips.stream()
					.filter(trip -> trip.isCancelled() && !trip.getDepartureAt().isBefore(leg.getArrivalAt())
							&& !trip.getDepartureAt().isAfter(next.getDepartureAt()))
					.findFirst().ifPresent(gone -> found.put(next.getId(), gone));
		}
		return found;
	}

	private static HourRow hourRow(LocalDate day, List<Trip> trips, List<Trip> live, Map<Long, Trip> broken, Trip end,
			LocalDateTime earliest, Map<Long, Integer> booked, LocalDateTime now) {
		LocalDateTime dayStart = day.atStartOfDay();
		LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
		boolean today = day.equals(now.toLocalDate());
		List<Trip> mine = trips.stream()
				.filter(trip -> trip.getDepartureAt().isBefore(dayEnd) && trip.getArrivalAt().isAfter(dayStart)).toList();
		boolean slotHere = end != null && !earliest.isBefore(dayStart) && earliest.isBefore(dayEnd);

		List<HourLeg> legs = new ArrayList<>();
		for (int i = 0; i < mine.size(); i++) {
			Trip trip = mine.get(i);
			LocalDateTime from = trip.getDepartureAt().isBefore(dayStart) ? dayStart : trip.getDepartureAt();
			LocalDateTime to = trip.getArrivalAt().isAfter(dayEnd) ? dayEnd : trip.getArrivalAt();
			// The words run on over the empty track, up to the next start of the day.
			LocalDateTime nextStart = dayEnd;
			if (i + 1 < mine.size()) {
				nextStart = mine.get(i + 1).getDepartureAt();
			}
			if (slotHere && earliest.isAfter(from) && earliest.isBefore(nextStart)) {
				nextStart = earliest;
			}
			legs.add(hourLeg(trip, from, to, nextStart, dayStart, dayEnd, broken.containsKey(trip.getId()),
					booked.getOrDefault(trip.getId(), 0), now));
		}

		// A wait between two legs inside this day, when the gap has room for its words.
		List<HourWait> waits = new ArrayList<>();
		List<Trip> liveHere = mine.stream().filter(trip -> !trip.isCancelled()).toList();
		for (int i = 1; i < liveHere.size(); i++) {
			Trip before = liveHere.get(i - 1);
			Trip after = liveHere.get(i);
			Duration gap = Duration.between(before.getArrivalAt(), after.getDepartureAt());
			if (gap.toHours() >= 3 && before.getRoute().getDestinationStation().getId().equals(after.getRoute().getOriginStation().getId())) {
				LocalDateTime a = before.getArrivalAt().isBefore(dayStart) ? dayStart : before.getArrivalAt();
				waits.add(new HourWait(percent((Duration.between(dayStart, a).toMillis()
						+ Duration.between(dayStart, after.getDepartureAt()).toMillis()) / 2), "chờ " + waitWords(gap)));
			}
		}

		String night = "";
		boolean nightRunning = false;
		Trip running = live.stream()
				.filter(trip -> trip.getDepartureAt().isBefore(dayEnd) && trip.getArrivalAt().isAfter(dayEnd)).findFirst().orElse(null);
		if (running != null) {
			night = "Đang chạy tới " + running.getRoute().getDestinationStation().getName();
			nightRunning = true;
		} else {
			Trip lastArrived = null;
			for (Trip trip : live) {
				if (!trip.getArrivalAt().isAfter(dayEnd)) {
					lastArrived = trip;
				}
			}
			if (lastArrived != null) {
				night = lastArrived.getRoute().getDestinationStation().getName();
			} else if (!live.isEmpty()) {
				night = live.get(0).getRoute().getOriginStation().getName();
			}
		}

		String alert = mine.stream().map(trip -> broken.get(trip.getId())).filter(gone -> gone != null).findFirst()
				.map(gone -> "Chuỗi chặng bị ngắt do chuyến " + gone.getRoute().getOriginStation().getName() + " → "
						+ gone.getRoute().getDestinationStation().getName() + " (" + when(gone.getDepartureAt()) + ") đã hủy.")
				.orElse(null);
		boolean past = day.isBefore(now.toLocalDate()) && mine.stream().allMatch(trip -> !trip.getArrivalAt().isAfter(now) || trip.isCancelled());
		String slotPercent = slotHere ? percent(Duration.between(dayStart, earliest).toMillis()) : null;
		String station = end == null ? "" : end.getRoute().getDestinationStation().getName();
		return new HourRow(dayTitle(day, now.toLocalDate()), day.format(DAY_MONTH), today, past, legs, waits,
				today ? percent(Duration.between(dayStart, now).toMillis()) : null, slotPercent,
				slotHere ? "Chặng mới " + earliest.format(TIME) : null,
				slotHere ? "Thêm chuyến từ " + station + ", sớm nhất " + when(earliest) : null, night, nightRunning, alert);
	}

	private static HourLeg hourLeg(Trip trip, LocalDateTime from, LocalDateTime to, LocalDateTime nextStart,
			LocalDateTime dayStart, LocalDateTime dayEnd, boolean chainBroken, int sold, LocalDateTime now) {
		String origin = trip.getRoute().getOriginStation().getName();
		String destination = trip.getRoute().getDestinationStation().getName();
		boolean continued = trip.getDepartureAt().isBefore(dayStart);
		boolean goesOn = trip.getArrivalAt().isAfter(dayEnd);
		String phase = trip.isCancelled() ? "is-off" : !trip.hasDeparted(now) ? "is-up" : trip.isRunning(now) ? "is-run" : "is-done";
		String css = phase + (continued ? " is-cont" : "") + (goesOn ? " is-on" : "") + (chainBroken ? " is-break" : "");
		int seats = CoachLayout.totalSeats();
		String aria = when(trip.getDepartureAt()) + ", " + origin + " đi " + destination + ", đến " + when(trip.getArrivalAt())
				+ ", " + (trip.isCancelled() ? "" : "đã bán " + sold + "/" + seats + ", ") + trip.phaseLabel(now)
				+ (chainBroken ? ", chuỗi chặng bị ngắt trước chuyến này" : "");
		boolean past = !trip.isCancelled() && !trip.getArrivalAt().isAfter(now);
		return new HourLeg(trip.getId(), css, past, percent(Duration.between(dayStart, from).toMillis()),
				percent(Duration.between(from, to).toMillis()), percent(Duration.between(from, nextStart).toMillis()),
				trip.isCancelled() ? null : (int) Math.round(sold * 100.0 / seats), aria, continued,
				continued ? "đến " + trip.getArrivalAt().format(TIME) : trip.getDepartureAt().format(TIME),
				continued ? destination : origin + " → " + destination, continued ? destination : "tới " + destination);
	}

	/** What the head of the page states: where the train is, its next trip, where a new leg starts, how its coming trips sell. */
	private static List<Fact> facts(List<Trip> live, Trip end, LocalDateTime earliest, Map<Long, Integer> booked,
			LocalDateTime now) {
		Trip running = live.stream().filter(trip -> trip.isRunning(now)).findFirst().orElse(null);
		Trip lastDone = null;
		List<Trip> ahead = new ArrayList<>();
		for (Trip trip : live) {
			if (!trip.getArrivalAt().isAfter(now)) {
				lastDone = trip;
			}
			if (trip.getDepartureAt().isAfter(now)) {
				ahead.add(trip);
			}
		}
		Fact position;
		if (running != null) {
			position = new Fact("Vị trí", running.getRoute().getOriginStation().getName() + " → " + running.getRoute().getDestinationStation().getName(),
					"Đang chạy, đến " + when(running.getArrivalAt()), null, null, true);
		} else if (lastDone != null) {
			position = new Fact("Vị trí", lastDone.getRoute().getDestinationStation().getName(), "Ở ga từ " + when(lastDone.getArrivalAt()), null, null, false);
		} else if (!live.isEmpty()) {
			position = new Fact("Vị trí", live.get(0).getRoute().getOriginStation().getName(), "Chưa chạy chặng nào", null, null, false);
		} else {
			position = new Fact("Vị trí", "Chưa có chuyến", null, null, null, false);
		}
		Trip next = ahead.isEmpty() ? null : ahead.get(0);
		Fact coming = next == null ? new Fact("Chuyến tới", "Chưa xếp", null, null, null, false)
				: new Fact("Chuyến tới", when(next.getDepartureAt()), next.getRoute().getOriginStation().getName() + " → "
						+ next.getRoute().getDestinationStation().getName() + ", sau " + waitWords(Duration.between(now, next.getDepartureAt())), null, null, false);
		Fact newLeg = end == null ? new Fact("Chặng mới đi từ", "Chọn ga xuất phát", "Tàu cần chặng đầu tiên", null, null, false)
				: new Fact("Chặng mới đi từ", end.getRoute().getDestinationStation().getName(), "Sớm nhất " + when(earliest),
						"Lịch xếp đến " + when(end.getArrivalAt()), null, false);
		long sold = ahead.stream().mapToLong(trip -> booked.getOrDefault(trip.getId(), 0)).sum();
		long seats = ahead.size() * (long) CoachLayout.totalSeats();
		int fill = seats == 0 ? 0 : (int) Math.round(sold * 100.0 / seats);
		Fact selling = seats == 0 ? new Fact("Đã bán (chuyến sắp chạy)", "Chưa có", null, null, null, false)
				: new Fact("Đã bán (chuyến sắp chạy)", fill + "%", sold + "/" + seats + " chỗ, " + ahead.size() + " chuyến", null, fill, false);
		return List.of(position, coming, newLeg, selling);
	}

	/** A span of a day as a percentage of it, with a dot whatever the locale. */
	private static String percent(long millis) {
		return String.format(Locale.ROOT, "%.3f", millis * 100.0 / Duration.ofDays(1).toMillis());
	}

	/** "08:00 29/10". */
	private static String when(LocalDateTime time) {
		return time.format(TIME) + " " + time.format(DAY_MONTH);
	}

	/** A wait in round words: "45 phút", "21 giờ", "1 ngày 17 giờ". */
	private static String waitWords(Duration span) {
		long hours = span.toHours();
		if (hours >= 24) {
			long rest = hours % 24;
			return hours / 24 + " ngày" + (rest > 0 ? " " + rest + " giờ" : "");
		}
		return hours >= 1 ? hours + " giờ" : Math.max(1, span.toMinutes()) + " phút";
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
