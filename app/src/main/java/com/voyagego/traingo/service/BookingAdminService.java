package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.BookingAdminDetail;
import com.voyagego.traingo.model.response.BookingAdminDetail.TicketLine;
import com.voyagego.traingo.model.response.BookingListView;
import com.voyagego.traingo.model.response.BookingListView.BookingRow;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;
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

	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
	private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final BookingRepository bookingRepository;
	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final BookingCancellationService cancellationService;

	public BookingAdminService(BookingRepository bookingRepository, TicketRepository ticketRepository,
			UserRepository userRepository, BookingCancellationService cancellationService) {
		this.bookingRepository = bookingRepository;
		this.ticketRepository = ticketRepository;
		this.userRepository = userRepository;
		this.cancellationService = cancellationService;
	}

	/**
	 * The rows of one group, earliest departure first. {@code q} matches the
	 * booking code, the customer's name or any passenger's name, ignoring case
	 * and diacritics. {@code customerId} limits the page to one customer.
	 */
	public BookingListView list(String tab, String q, Long customerId, LocalDateTime now) {
		Map<Group, List<Booking>> byGroup = bookingRepository.findAllForAdmin(customerId).stream()
				.collect(Collectors.groupingBy(booking -> groupOf(booking, now)));
		Group open = tab == null && customerId != null ? firstNonEmpty(byGroup) : Group.of(tab);
		List<Booking> inGroup = byGroup.getOrDefault(open, List.of());

		String typed = q == null ? "" : q.strip();
		String needle = TextUtils.fold(typed);
		// Passengers are only read when there is something to search for.
		Map<Long, List<Ticket>> ticketsByBooking = needle.isEmpty() ? Map.of() : ticketsOf(inGroup);
		Map<Long, Long> ticketCounts = ticketRepository.countByBooking().stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));

		List<BookingRow> rows = inGroup.stream()
				.filter(booking -> needle.isEmpty()
						|| matches(booking, needle, ticketsByBooking.getOrDefault(booking.getId(), List.of())))
				.sorted(Comparator.comparing((Booking booking) -> booking.getTrip().getDepartureAt()))
				.map(booking -> toRow(booking, ticketCounts.getOrDefault(booking.getId(), 0L).intValue()))
				.toList();

		String customerName = customerId == null ? ""
				: userRepository.findById(customerId).map(user -> user.getFullName()).orElse("");
		// With nothing typed, a customer's page shows the customer's name in the box.
		// The customer filter already narrows the list, so the name filters nothing.
		String shown = typed.isEmpty() ? customerName : typed;
		return new BookingListView(open.key(), shown, !typed.isEmpty(), customerId, customerName,
				byGroup.getOrDefault(Group.UPCOMING, List.of()).size(),
				byGroup.getOrDefault(Group.PAST, List.of()).size(),
				byGroup.getOrDefault(Group.CANCELLED, List.of()).size(), rows);
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

	private static BookingRow toRow(Booking booking, int ticketCount) {
		Trip trip = booking.getTrip();
		return new BookingRow(booking.getBookingCode(), booking.getCustomer().getFullName(),
				booking.getCustomer().getEmail(), trip.getTrain().getCode(), routeLabel(trip),
				weekdayLabel(trip.getDepartureAt()), ticketCount, booking.getTotalPrice());
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
