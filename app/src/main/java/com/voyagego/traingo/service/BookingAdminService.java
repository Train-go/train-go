package com.voyagego.traingo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.TextUtils;
import com.voyagego.traingo.common.exception.BookingNotFoundException;
import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.PaymentStatus;
import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.BookingAdminDetail;
import com.voyagego.traingo.model.response.BookingAdminDetail.TicketLine;
import com.voyagego.traingo.model.response.BookingListView;
import com.voyagego.traingo.model.response.BookingListView.BookingLine;
import com.voyagego.traingo.model.response.BookingListView.Passenger;
import com.voyagego.traingo.model.response.BookingListView.TripGroup;
import com.voyagego.traingo.model.response.CoachFill;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;
import com.voyagego.traingo.repository.TripSeatRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * The admin's view of bookings: the three groups of the Booking page and the
 * detail sheet. A cancelled booking is "Đã hủy"; otherwise the trip's
 * departure decides: after {@code now} is "Sắp đi", anything else "Đã đi".
 * Every method takes {@code now} from its caller, so one page shows one moment.
 */
@Service
@Transactional(readOnly = true)
public class BookingAdminService {

	/** The groups of the Booking page; {@link #key} is the value of ?tab=. */
	public enum Group {
		UPCOMING("upcoming"), PAST("past"), CANCELLED("cancelled");

		private final String key;

		Group(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		/** An unknown or missing tab opens the first group. */
		static Group of(String key) {
			for (Group group : values()) {
				if (group.key.equals(key)) {
					return group;
				}
			}
			return UPCOMING;
		}
	}

	/** Trips a page of the list shows, and the most "Hiện thêm" can ask for. */
	public static final int PAGE = 10;
	private static final int MAX_SHOWN = 1000;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
	private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final BookingRepository bookingRepository;
	private final TicketRepository ticketRepository;
	private final TripSeatRepository tripSeatRepository;
	private final UserRepository userRepository;
	private final BookingCancellationService cancellationService;

	public BookingAdminService(BookingRepository bookingRepository, TicketRepository ticketRepository,
			TripSeatRepository tripSeatRepository, UserRepository userRepository, BookingCancellationService cancellationService) {
		this.bookingRepository = bookingRepository;
		this.ticketRepository = ticketRepository;
		this.tripSeatRepository = tripSeatRepository;
		this.userRepository = userRepository;
		this.cancellationService = cancellationService;
	}

	/**
	 * One group, as trips with their bookings: earliest departure first while
	 * upcoming, latest first once gone. {@code q} matches the booking code, the
	 * customer's name or any passenger's name, ignoring case and diacritics.
	 * {@code customerId} limits the page to one customer. Only the first
	 * {@code limit} trips are read further, so the cost follows the page.
	 */
	public BookingListView list(String tab, String q, Long customerId, int limit, LocalDateTime now) {
		Map<Group, List<Booking>> byGroup = bookingRepository.findAllForAdmin(customerId).stream()
				.collect(Collectors.groupingBy(booking -> groupOf(booking, now)));
		Group open = tab == null && customerId != null ? firstNonEmpty(byGroup) : Group.of(tab);
		List<Booking> inGroup = byGroup.getOrDefault(open, List.of());

		String typed = q == null ? "" : q.strip();
		String needle = TextUtils.fold(typed);
		// Passengers are only read for all of the group when there is something to search for.
		Map<Long, List<Ticket>> searched = needle.isEmpty() ? Map.of() : ticketsOf(inGroup);
		Comparator<Booking> byDeparture = Comparator.comparing((Booking booking) -> booking.getTrip().getDepartureAt());
		Map<Long, List<Booking>> byTrip = inGroup.stream()
				.filter(booking -> needle.isEmpty()
						|| matches(booking, needle, searched.getOrDefault(booking.getId(), List.of())))
				.sorted(open == Group.UPCOMING ? byDeparture : byDeparture.reversed())
				.collect(Collectors.groupingBy(booking -> booking.getTrip().getId(), LinkedHashMap::new, Collectors.toList()));

		int shown = Math.max(PAGE, Math.min(limit, MAX_SHOWN));
		boolean searching = !typed.isEmpty();
		List<TripGroup> trips = toGroups(byTrip.values().stream().limit(shown).toList(), open, searching, now);
		String customerName = customerId == null ? ""
				: userRepository.findById(customerId).map(user -> user.getFullName()).orElse("");
		return new BookingListView(open.key(), typed, searching, customerId, customerName, shown, byTrip.size(),
				byGroup.getOrDefault(Group.UPCOMING, List.of()).size(),
				byGroup.getOrDefault(Group.PAST, List.of()).size(),
				byGroup.getOrDefault(Group.CANCELLED, List.of()).size(), trips);
	}

	private List<TripGroup> toGroups(List<List<Booking>> pages, Group open, boolean searching, LocalDateTime now) {
		if (pages.isEmpty()) {
			return List.of();
		}
		List<Long> tripIds = pages.stream().map(books -> books.get(0).getTrip().getId()).toList();
		Map<Long, List<Ticket>> tickets = ticketsOf(pages.stream().flatMap(List::stream).toList());
		Map<Long, Long> sold = ticketRepository.countSoldByTrips(tripIds).stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		Map<Long, List<CoachFill>> fills = CoachFill.byTrip(tripSeatRepository.countBookedByTripAndCoach(tripIds));
		int seats = CoachLayout.totalSeats();
		List<TripGroup> groups = new ArrayList<>();
		LocalDate previous = null;
		for (List<Booking> books : pages) {
			Trip trip = books.get(0).getTrip();
			LocalDate day = trip.getDepartureAt().toLocalDate();
			long soldSeats = sold.getOrDefault(trip.getId(), 0L);
			String count = soldSeats + "/" + seats + " chỗ đã bán";
			String summary = open == Group.CANCELLED ? books.size() + " booking đã hủy"
					: searching ? books.size() + " booking khớp tìm kiếm, " + count : count;
			boolean bar = open != Group.CANCELLED && !trip.isCancelled();
			groups.add(new TripGroup(trip.getId(), day.equals(previous) ? null : TripAdminService.dayLabel(day, now.toLocalDate()),
					day.format(DATE), trip.getDepartureAt().format(TIME), trip.getTrain().getCode(), routeLabel(trip), summary,
					bar ? fills.getOrDefault(trip.getId(), CoachFill.of(Map.of())) : null,
					books.stream().map(booking -> toLine(booking, tickets.getOrDefault(booking.getId(), List.of()))).toList()));
			previous = day;
		}
		return groups;
	}

	private static BookingLine toLine(Booking booking, List<Ticket> tickets) {
		boolean cancelled = booking.getBookingStatus() == BookingStatus.CANCELLED;
		return new BookingLine(booking.getBookingCode(), booking.getCustomer().getFullName(),
				booking.getCustomer().getEmail(), booking.getTotalPrice(), cancelled,
				tickets.stream().map(ticket -> new Passenger(ticket.getFullName(), cancelled ? "Đã trả chỗ"
						: "Toa " + ticket.getTripSeat().getCoachNumber() + ", " + ticket.getTripSeat().getSeatCode())).toList());
	}

	/** Throws BookingNotFoundException, shown as a 404, for an unknown code. */
	public BookingAdminDetail detail(String code, LocalDateTime now) {
		Booking booking = bookingRepository.findByBookingCode(code).orElseThrow(BookingNotFoundException::new);
		Trip trip = booking.getTrip();
		boolean cancelled = booking.getBookingStatus() == BookingStatus.CANCELLED;
		List<TicketLine> tickets = ticketRepository.findByBookingIds(List.of(booking.getId())).stream()
				.map(ticket -> new TicketLine(ticket.getTicketCode(), ticket.getFullName(),
						TextUtils.maskId(ticket.getIdentityNumber()),
						"Toa " + ticket.getTripSeat().getCoachNumber() + " · " + ticket.getTripSeat().getSeatCode(),
						ticket.getTicketPrice()))
				.toList();
		return new BookingAdminDetail(booking.getBookingCode(), cancelled, statusLabel(booking), statusCss(booking),
				booking.getCustomer().getFullName(), booking.getCustomer().getEmail(),
				booking.getCustomer().getPhoneNumber(), trip.getId(), trip.getTrain().getCode(), routeLabel(trip),
				weekdayLabel(trip.getDepartureAt()), booking.getTotalPrice(),
				booking.getPaidAt() == null ? null : booking.getPaidAt().format(FULL), tickets,
				cancelled ? null : BookingCancellationService.blockReason(booking, now));
	}

	/**
	 * Cancels a booking on the admin's behalf, with the same rule as the
	 * customer's own cancel, and returns how many seats went back on sale.
	 * Throws BookingNotFoundException for an unknown code and
	 * BookingCancelBlockedException when the booking cannot be cancelled.
	 */
	@Transactional
	public int cancel(String code, LocalDateTime now) {
		// Only the id is read here: loading the booking before the cancellation service
		// locks it would leave a stale copy in the persistence context, and the check
		// after the lock would not see a cancel that committed in between.
		long id = bookingRepository.findIdByBookingCode(code).orElseThrow(BookingNotFoundException::new);
		cancellationService.cancel(id, now);
		return ticketRepository.findByBookingIds(List.of(id)).size();
	}

	/** The first group with a booking, so a customer's page never opens on an empty tab. */
	private static Group firstNonEmpty(Map<Group, List<Booking>> byGroup) {
		for (Group group : Group.values()) {
			if (!byGroup.getOrDefault(group, List.of()).isEmpty()) {
				return group;
			}
		}
		return Group.UPCOMING;
	}

	private static Group groupOf(Booking booking, LocalDateTime now) {
		if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
			return Group.CANCELLED;
		}
		return booking.getTrip().getDepartureAt().isAfter(now) ? Group.UPCOMING : Group.PAST;
	}

	/** Passengers per booking id, for the bookings being searched. */
	private Map<Long, List<Ticket>> ticketsOf(List<Booking> bookings) {
		if (bookings.isEmpty()) {
			return Map.of();
		}
		return ticketRepository.findByBookingIds(bookings.stream().map(Booking::getId).toList()).stream()
				.collect(Collectors.groupingBy(ticket -> ticket.getBooking().getId()));
	}

	private static boolean matches(Booking booking, String needle, List<Ticket> tickets) {
		return TextUtils.fold(booking.getBookingCode()).contains(needle)
				|| TextUtils.fold(booking.getCustomer().getFullName()).contains(needle)
				|| tickets.stream().anyMatch(ticket -> TextUtils.fold(ticket.getFullName()).contains(needle));
	}

	private static String routeLabel(Trip trip) {
		return trip.getRoute().getOriginStation().getName() + " → " + trip.getRoute().getDestinationStation().getName();
	}

	/** "T5, 22/10 08:00". */
	private static String weekdayLabel(LocalDateTime time) {
		return SHORT_WEEKDAYS[time.getDayOfWeek().getValue() - 1] + ", " + time.format(DAY_TIME);
	}

	/** A cancelled booking reads as cancelled whatever its payment status says. */
	private static String statusLabel(Booking booking) {
		if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
			return "Đã hủy";
		}
		return booking.getPaymentStatus() == PaymentStatus.PAID ? "Đã thanh toán" : "Chờ thanh toán";
	}

	/** The .tag modifiers in css/admin/admin.css. */
	private static String statusCss(Booking booking) {
		if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
			return "cancelled";
		}
		return booking.getPaymentStatus() == PaymentStatus.PAID ? "ok" : "running";
	}

}
