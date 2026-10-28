package com.voyagego.traingo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.dto.DayCount;
import com.voyagego.traingo.model.dto.DayTotal;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.dto.TrainDayCount;
import com.voyagego.traingo.model.dto.TrainSchedule;
import com.voyagego.traingo.model.response.Overview;
import com.voyagego.traingo.model.response.Overview.Alert;
import com.voyagego.traingo.model.response.Overview.Trend;
import com.voyagego.traingo.model.response.Overview.TrainWeek;
import com.voyagego.traingo.model.response.Overview.WeekDay;
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.model.response.SalesSeries;
import com.voyagego.traingo.model.response.UsersSeries;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * The numbers on /admin (#37). A cancelled booking stays PAID in the database,
 * but the project has no refund flow, so only CONFIRMED bookings count as
 * bookings and as revenue.
 *
 * The page also looks ahead: which trains are about to run out of scheduled
 * trips or have empty days this week, which trips of the next 48 hours are
 * selling badly or nearly full, and what the bookings already made are worth
 * per day of the coming week. Every figure comes from one grouped query, so
 * the cost does not grow with the number of bookings.
 */
@Service
@Transactional(readOnly = true)
public class OverviewService {

	/** Days the train grid and the forecast look ahead, today included. */
	public static final int WEEK = 7;
	static final int[] RANGES = { 7, 14, 30 };
	static final int DEFAULT_RANGE = 14;
	/** Trips leaving within this many hours are checked for how full they are. */
	static final int FILL_WINDOW_HOURS = 48;
	/** Under this share of seats sold a trip about to leave is flagged. */
	static final int LOW_FILL_PERCENT = 30;
	static final int FULL_FILL_PERCENT = 90;

	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

	private final BookingRepository bookingRepository;
	private final TripRepository tripRepository;
	private final UserRepository userRepository;
	private final TrainRepository trainRepository;
	private final TicketRepository ticketRepository;

	public OverviewService(BookingRepository bookingRepository, TripRepository tripRepository,
			UserRepository userRepository, TrainRepository trainRepository, TicketRepository ticketRepository) {
		this.bookingRepository = bookingRepository;
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
		this.trainRepository = trainRepository;
		this.ticketRepository = ticketRepository;
	}

	public Overview overview(LocalDateTime now) {
		LocalDate today = now.toLocalDate();
		List<WeekDay> week = week(today);
		List<TrainWeek> trains = new ArrayList<>();
		List<Alert> alerts = new ArrayList<>(trainAlerts(now, week, trains));
		alerts.addAll(tripAlerts(now));
		List<DayTotal> lastTwoWeeks = bookingRepository.findSalesByDay(
				today.minusDays(2L * WEEK - 1).atStartOfDay(), today.plusDays(1).atStartOfDay());
		alerts.sort(Comparator.comparingInt(alert -> severity(alert.level())));

		trains.sort(Comparator.comparingInt((TrainWeek train) -> severity(train.level()))
				.thenComparing(Comparator.comparingLong((TrainWeek train) -> train.trips().stream().filter(n -> n == 0).count()).reversed())
				.thenComparing(TrainWeek::code));

		return new Overview(
				bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED),
				bookingRepository.countByBookingStatus(BookingStatus.CANCELLED),
				tripRepository.countByStatusNot(TripStatus.CANCELLED),
				tripRepository.countByStatusAndDepartureAtAfter(TripStatus.SCHEDULED, now),
				userRepository.countByRole(UserRole.CUSTOMER),
				bookingRepository.sumTotalPriceByBookingStatus(BookingStatus.CONFIRMED),
				trend(lastTwoWeeks, today, DayTotal::getBookings), trend(lastTwoWeeks, today, DayTotal::getRevenue),
				usersTrend(today),
				alerts.stream().limit(Overview.MAX_ALERTS).toList(), alerts.size(),
				week, trains.stream().limit(Overview.MAX_TRAINS).toList(), trains.size());
	}

	/** Bookings and revenue per day for the last {@code days} days (7, 14 or 30; anything else is 14). */
	public SalesSeries sales(int days, LocalDateTime now) {
		int range = rangeOf(days);
		LocalDate today = now.toLocalDate();
		LocalDate first = today.minusDays(range - 1L);
		Map<LocalDate, DayTotal> byDay = bookingRepository
				.findSalesByDay(first.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
				.collect(Collectors.toMap(DayTotal::getDay, total -> total));
		List<SalesSeries.Point> points = new ArrayList<>();
		long bookings = 0;
		long revenue = 0;
		for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
			DayTotal total = byDay.get(day);
			long b = total == null ? 0 : total.getBookings();
			long r = total == null ? 0 : total.getRevenue();
			bookings += b;
			revenue += r;
			points.add(new SalesSeries.Point(day.toString(), day.format(DAY_MONTH), b, r));
		}
		return new SalesSeries(range, points, bookings, revenue);
	}

	/** Customers registered per day for the last {@code days} days (7, 14 or 30; anything else is 14). */
	public UsersSeries users(int days, LocalDateTime now) {
		int range = rangeOf(days);
		LocalDate today = now.toLocalDate();
		LocalDate first = today.minusDays(range - 1L);
		Map<LocalDate, Long> byDay = userRepository
				.findSignupsByDay(first.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
				.collect(Collectors.toMap(DayCount::getDay, DayCount::getTotal));
		long total = userRepository.countByRoleAndCreatedAtBefore(UserRole.CUSTOMER, first.atStartOfDay());
		long newUsers = 0;
		List<UsersSeries.Point> points = new ArrayList<>();
		for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
			long created = byDay.getOrDefault(day, 0L);
			newUsers += created;
			total += created;
			points.add(new UsersSeries.Point(day.toString(), day.format(DAY_MONTH), created, total));
		}
		return new UsersSeries(range, points, newUsers, total);
	}

	/**
	 * One alert per train that is out of trips, about to be, or has empty days
	 * this week, and one row per train for the grid.
	 */
	private List<Alert> trainAlerts(LocalDateTime now, List<WeekDay> week, List<TrainWeek> rows) {
		LocalDate today = now.toLocalDate();
		LocalDate weekEnd = today.plusDays(WEEK);
		Map<Long, Map<LocalDate, Long>> counts = new HashMap<>();
		for (TrainDayCount count : tripRepository.findTrainDayCounts(today.atStartOfDay(), weekEnd.atStartOfDay())) {
			counts.computeIfAbsent(count.getTrainId(), id -> new HashMap<>()).put(count.getDay(), count.getTrips());
		}
		Map<Long, TrainSchedule> schedules = tripRepository.findTrainSchedules(now).stream()
				.collect(Collectors.toMap(TrainSchedule::getTrainId, schedule -> schedule));

		List<Alert> alerts = new ArrayList<>();
		for (Train train : trainRepository.findAllByDeletedFalseOrderByCodeAsc()) {
			Map<LocalDate, Long> perDay = counts.getOrDefault(train.getId(), Map.of());
			List<Integer> trips = week.stream().map(day -> perDay.getOrDefault(day.date(), 0L).intValue()).toList();
			List<String> emptyDays = week.stream().filter(day -> perDay.getOrDefault(day.date(), 0L) == 0)
					.map(WeekDay::dayMonth).toList();
			TrainSchedule schedule = schedules.get(train.getId());
			String href = "/admin/trains/" + train.getId();
			String level = null;
			if (schedule == null) {
				level = "danger";
				alerts.add(new Alert(level, "bi-exclamation-octagon", train.getCode() + " hết lịch chạy",
						"Không còn chuyến nào sắp chạy. Thêm chuyến để tàu tiếp tục khai thác.", href));
			} else if (schedule.getLastAt().toLocalDate().isBefore(weekEnd)) {
				level = "warning";
				alerts.add(new Alert(level, "bi-hourglass-split", train.getCode() + " sắp hết chuyến",
						"Chuyến cuối khởi hành " + schedule.getLastAt().format(DAY_TIME) + ", lịch còn "
								+ schedule.getUpcoming() + " chuyến.", href));
			} else if (!emptyDays.isEmpty()) {
				level = "info";
				alerts.add(new Alert(level, "bi-calendar-x", train.getCode() + " có ngày chưa có chuyến",
						emptyDays.size() == WEEK ? "Không có chuyến nào trong 7 ngày tới."
								: "Chưa có chuyến ngày " + String.join(", ", emptyDays) + ".", href));
			}
			rows.add(new TrainWeek(train.getId(), train.getCode(), train.getName(), trips, level));
		}
		return alerts;
	}

	/** Trips leaving in the next 48 hours that sold under 30% or over 90% of their seats. */
	private List<Alert> tripAlerts(LocalDateTime now) {
		List<Trip> trips = tripRepository.findUpcomingBetween(now, now.plusHours(FILL_WINDOW_HOURS));
		if (trips.isEmpty()) {
			return List.of();
		}
		Map<Long, Long> sold = ticketRepository.countSoldByTrips(trips.stream().map(Trip::getId).toList()).stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		long seats = CoachLayout.totalSeats();
		List<Alert> low = new ArrayList<>();
		List<Alert> full = new ArrayList<>();
		trips.stream().sorted(Comparator.comparingLong((Trip trip) -> sold.getOrDefault(trip.getId(), 0L)).thenComparing(Trip::getDepartureAt))
				.forEach(trip -> {
					long count = sold.getOrDefault(trip.getId(), 0L);
					int fill = percent(count, seats);
					String route = PopularRoute.placeName(trip.getRoute().getOriginStation().getName()) + " → "
							+ PopularRoute.placeName(trip.getRoute().getDestinationStation().getName());
					String href = "/admin/trains/" + trip.getTrain().getId();
					String when = "Khởi hành " + trip.getDepartureAt().format(DAY_TIME);
					if (fill < LOW_FILL_PERCENT) {
						low.add(new Alert("warning", "bi-graph-down-arrow",
								trip.getTrain().getCode() + " " + route + " còn trống nhiều",
								when + ", mới bán " + count + "/" + seats + " chỗ.", href));
					} else if (fill >= FULL_FILL_PERCENT) {
						full.add(new Alert("info", "bi-graph-up-arrow",
								trip.getTrain().getCode() + " " + route + " sắp đầy",
								when + ", đã bán " + count + "/" + seats + " chỗ.", href));
					}
				});
		List<Alert> result = new ArrayList<>(low.stream().limit(5).toList());
		result.addAll(full.stream().limit(3).toList());
		return result;
	}

	/** The last 7 days (today included) against the 7 days before, for one figure of the sales rows. */
	private static Trend trend(List<DayTotal> lastTwoWeeks, LocalDate today, ToLongFunction<DayTotal> figure) {
		long current = 0;
		long previous = 0;
		for (DayTotal total : lastTwoWeeks) {
			if (total.getDay().isAfter(today.minusDays(WEEK))) current += figure.applyAsLong(total);
			else previous += figure.applyAsLong(total);
		}
		return new Trend(current, previous);
	}

	/** Customers registered in the last 7 days against the 7 days before. */
	private Trend usersTrend(LocalDate today) {
		long current = 0;
		long previous = 0;
		LocalDate from = today.minusDays(2L * WEEK - 1);
		for (DayCount count : userRepository.findSignupsByDay(from.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
			if (count.getDay().isAfter(today.minusDays(WEEK))) current += count.getTotal();
			else previous += count.getTotal();
		}
		return new Trend(current, previous);
	}

	private static int rangeOf(int days) {
		return Arrays.stream(RANGES).anyMatch(allowed -> allowed == days) ? days : DEFAULT_RANGE;
	}

	private static List<WeekDay> week(LocalDate today) {
		List<WeekDay> days = new ArrayList<>();
		for (int i = 0; i < WEEK; i++) {
			LocalDate day = today.plusDays(i);
			days.add(new WeekDay(day, shortWeekday(day), day.format(DAY_MONTH), i == 0));
		}
		return days;
	}

	private static String shortWeekday(LocalDate day) {
		return SHORT_WEEKDAYS[day.getDayOfWeek().getValue() - 1];
	}

	private static int percent(long part, long whole) {
		return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
	}

	/** danger first, then warning, then info, then none. */
	private static int severity(String level) {
		return level == null ? 3 : switch (level) {
			case "danger" -> 0;
			case "warning" -> 1;
			default -> 2;
		};
	}

}
