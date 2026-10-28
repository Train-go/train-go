package com.voyagego.traingo.service;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.InvalidRangeException;
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
	/** The longest range a chart may cover, a year. */
	public static final int MAX_RANGE_DAYS = 366;
	/** Trips leaving within this many hours are checked for how full they are. */
	static final int FILL_WINDOW_HOURS = 48;
	/** Under this share of seats sold a trip about to leave is flagged. */
	static final int LOW_FILL_PERCENT = 30;
	static final int FULL_FILL_PERCENT = 90;

	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MM/yyyy");

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

	/** Bookings and revenue over a date range, grouped as asked ({@link Range}). */
	public SalesSeries sales(LocalDate from, LocalDate to, String group, LocalDateTime now) {
		Range range = Range.of(from, to, group, now.toLocalDate());
		Map<LocalDate, DayTotal> byDay = bookingRepository
				.findSalesByDay(range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay()).stream()
				.collect(Collectors.toMap(DayTotal::getDay, total -> total));
		Map<LocalDate, long[]> sums = new LinkedHashMap<>();
		range.forEachBucket(sums, (acc, day) -> {
			DayTotal total = byDay.get(day);
			if (total != null) {
				acc[0] += total.getBookings();
				acc[1] += total.getRevenue();
			}
		});
		List<SalesSeries.Point> points = new ArrayList<>();
		long bookings = 0;
		long revenue = 0;
		for (Map.Entry<LocalDate, long[]> entry : sums.entrySet()) {
			long[] acc = entry.getValue();
			bookings += acc[0];
			revenue += acc[1];
			points.add(new SalesSeries.Point(entry.getKey().toString(), range.label(entry.getKey()),
					range.title(entry.getKey()), acc[0], acc[1]));
		}
		return new SalesSeries(range.from(), range.to(), range.group(), points, bookings, revenue);
	}

	/** Customers registered over a date range, grouped as asked, with the running total. */
	public UsersSeries users(LocalDate from, LocalDate to, String group, LocalDateTime now) {
		Range range = Range.of(from, to, group, now.toLocalDate());
		Map<LocalDate, Long> byDay = userRepository
				.findSignupsByDay(range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay()).stream()
				.collect(Collectors.toMap(DayCount::getDay, DayCount::getTotal));
		Map<LocalDate, long[]> sums = new LinkedHashMap<>();
		range.forEachBucket(sums, (acc, day) -> acc[0] += byDay.getOrDefault(day, 0L));
		long total = userRepository.countByRoleAndCreatedAtBefore(UserRole.CUSTOMER, range.from().atStartOfDay());
		long newUsers = 0;
		List<UsersSeries.Point> points = new ArrayList<>();
		for (Map.Entry<LocalDate, long[]> entry : sums.entrySet()) {
			newUsers += entry.getValue()[0];
			total += entry.getValue()[0];
			points.add(new UsersSeries.Point(entry.getKey().toString(), range.label(entry.getKey()),
					range.title(entry.getKey()), entry.getValue()[0], total));
		}
		return new UsersSeries(range.from(), range.to(), range.group(), points, newUsers, total);
	}

	/**
	 * The date range of a chart, settled: an empty end is today, an empty start
	 * is 29 days before the end, the end is never after today, the range is at
	 * most {@link #MAX_RANGE_DAYS} days, and the grouping is day, week or month
	 * (anything else picks one that keeps the chart readable). A week starts on
	 * Monday; the first and last bucket are cut at the range.
	 */
	record Range(LocalDate from, LocalDate to, String group) {

		static Range of(LocalDate from, LocalDate to, String group, LocalDate today) {
			LocalDate end = to == null || to.isAfter(today) ? today : to;
			LocalDate start = from == null ? end.minusDays(29) : from;
			if (start.isAfter(end)) {
				throw new InvalidRangeException("Ngày bắt đầu phải trước hoặc cùng ngày kết thúc.");
			}
			long days = ChronoUnit.DAYS.between(start, end) + 1;
			if (days > MAX_RANGE_DAYS) {
				throw new InvalidRangeException("Chọn tối đa " + MAX_RANGE_DAYS + " ngày.");
			}
			String grouping = "day".equals(group) || "week".equals(group) || "month".equals(group) ? group
					: days <= 31 ? "day" : days <= 180 ? "week" : "month";
			return new Range(start, end, grouping);
		}

		LocalDate bucketOf(LocalDate day) {
			return switch (group) {
				case "week" -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				case "month" -> day.withDayOfMonth(1);
				default -> day;
			};
		}

		/** Walks every day of the range, in order, handing each to {@code add} with its bucket's accumulator. */
		void forEachBucket(Map<LocalDate, long[]> buckets, BiConsumer<long[], LocalDate> add) {
			for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
				add.accept(buckets.computeIfAbsent(bucketOf(day), key -> new long[2]), day);
			}
		}

		/** First and last day of a bucket inside the range. */
		private LocalDate[] span(LocalDate bucket) {
			LocalDate last = switch (group) {
				case "week" -> bucket.plusDays(6);
				case "month" -> bucket.withDayOfMonth(bucket.lengthOfMonth());
				default -> bucket;
			};
			return new LocalDate[] { bucket.isBefore(from) ? from : bucket, last.isAfter(to) ? to : last };
		}

		String label(LocalDate bucket) {
			return "month".equals(group) ? bucket.format(MONTH_YEAR) : span(bucket)[0].format(DAY_MONTH);
		}

		String title(LocalDate bucket) {
			LocalDate[] span = span(bucket);
			return switch (group) {
				case "week" -> "Tuần " + span[0].format(DAY_MONTH) + " - " + span[1].format(DAY_MONTH);
				case "month" -> "Tháng " + bucket.format(MONTH_YEAR);
				default -> span[0].format(DAY_MONTH_YEAR);
			};
		}

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
				alerts.add(new Alert(level, "bi-exclamation-octagon", train.getCode() + " chưa có lịch chạy sắp tới",
						"Hiện không có chuyến nào được lên lịch. Hãy bổ sung lịch chạy để tiếp tục khai thác.", href));
			} else if (schedule.getLastAt().toLocalDate().isBefore(weekEnd)) {
				level = "warning";
				alerts.add(new Alert(level, "bi-hourglass-split", train.getCode() + " sắp hết lịch chạy",
						"Chuyến cuối khởi hành " + when(schedule.getLastAt()) + ". Lịch hiện còn "
								+ schedule.getUpcoming() + " chuyến, hãy bổ sung lịch chạy.", href));
			} else if (!emptyDays.isEmpty()) {
				level = "info";
				alerts.add(new Alert(level, "bi-calendar-x", train.getCode() + " có ngày chưa có chuyến",
						emptyDays.size() == WEEK ? "Tàu chưa có chuyến nào trong 7 ngày tới."
								: "Chưa có chuyến vào ngày " + String.join(", ", emptyDays) + ".", href));
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
					String title = trip.getTrain().getCode() + " · " + route;
					String start = "Khởi hành " + when(trip.getDepartureAt()) + ". ";
					if (fill < LOW_FILL_PERCENT) {
						low.add(new Alert("warning", "bi-graph-down-arrow",
								title + (count == 0 ? " chưa bán được vé" : " có tỷ lệ lấp đầy thấp"),
								start + (count == 0 ? "Hiện còn trống toàn bộ " + seats + " chỗ."
										: "Mới bán " + count + "/" + seats + " chỗ, còn " + (seats - count) + " chỗ trống."),
								href));
					} else if (fill >= FULL_FILL_PERCENT) {
						full.add(new Alert("info", "bi-graph-up-arrow", title + " sắp hết chỗ",
								start + "Đã bán " + count + "/" + seats + " chỗ, chỉ còn " + (seats - count) + " chỗ trống.",
								href));
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

	private static List<WeekDay> week(LocalDate today) {
		List<WeekDay> days = new ArrayList<>();
		for (int i = 0; i < WEEK; i++) {
			LocalDate day = today.plusDays(i);
			days.add(new WeekDay(day, shortWeekday(day), day.format(DAY_MONTH), i == 0));
		}
		return days;
	}

	/** "lúc 08:00 ngày 29/10", the way an alert names a departure. */
	private static String when(LocalDateTime time) {
		return "lúc " + time.format(HOUR_MINUTE) + " ngày " + time.format(DAY_MONTH);
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
