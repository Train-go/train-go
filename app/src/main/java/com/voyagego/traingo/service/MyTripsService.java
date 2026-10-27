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
import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.response.BookingDetail;
import com.voyagego.traingo.model.response.BookingDetail.TicketLine;
import com.voyagego.traingo.model.response.MyTripsView;
import com.voyagego.traingo.model.response.MyTripsView.BookingCard;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;

/**
 * The customer's own bookings, "Vé của tôi" in docs/product/booking-flow.md.
 * Every method takes the signed-in customer's id and only ever reads that
 * customer's bookings: someone else's booking reads as one that does not
 * exist (BookingNotFoundException, a 404), so its code is never revealed
 * (docs/product/auth.md).
 *
 * The three groups are the ones of the admin Booking page (D8): cancelled,
 * then upcoming while the trip's departure is after now, otherwise past.
 * Cancelling goes through BookingCancellationService, the one place that
 * holds the cancel rule.
 */
@Service
public class MyTripsService {

	public static final String TAB_UPCOMING = "upcoming";
	public static final String TAB_PAST = "past";
	public static final String TAB_CANCELLED = "cancelled";

	/** Indexed by DayOfWeek.getValue() - 1, Monday first. */
	private static final String[] WEEKDAYS = { "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy",
			"Chủ nhật" };
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final BookingRepository bookingRepository;
	private final TicketRepository ticketRepository;
	private final BookingCancellationService cancellationService;

	public MyTripsService(BookingRepository bookingRepository, TicketRepository ticketRepository,
			BookingCancellationService cancellationService) {
		this.bookingRepository = bookingRepository;
		this.ticketRepository = ticketRepository;
		this.cancellationService = cancellationService;
	}

	/**
	 * One group of the customer's bookings. An unknown tab opens upcoming.
	 * Upcoming trips come soonest first; past and cancelled ones latest first.
	 */
	@Transactional(readOnly = true)
	public MyTripsView list(long customerId, String tab, LocalDateTime now) {
		String selected = switch (tab == null ? "" : tab) {
			case TAB_PAST, TAB_CANCELLED -> tab;
			default -> TAB_UPCOMING;
		};
		Map<String, List<Booking>> byGroup = bookingRepository.findAllForAdmin(customerId).stream()
				.collect(Collectors.groupingBy(booking -> groupOf(booking, now)));
		Comparator<Booking> byDeparture = Comparator.comparing(booking -> booking.getTrip().getDepartureAt());
		List<Booking> shown = byGroup.getOrDefault(selected, List.of()).stream()
				.sorted(TAB_UPCOMING.equals(selected) ? byDeparture : byDeparture.reversed())
				.toList();

		Map<Long, List<Ticket>> ticketsByBooking = shown.isEmpty() ? Map.of()
				: ticketRepository.findByBookingIds(shown.stream().map(Booking::getId).toList()).stream()
						.collect(Collectors.groupingBy(ticket -> ticket.getBooking().getId()));
		List<BookingCard> cards = shown.stream().map(booking -> {
			List<Ticket> tickets = ticketsByBooking.getOrDefault(booking.getId(), List.of());
			Trip trip = booking.getTrip();
			return new BookingCard(booking.getId(), booking.getBookingCode(), routeLabel(trip),
					departureLabel(trip), trip.getTrain().getCode(),
					tickets.stream().map(MyTripsService::seatLabel).collect(Collectors.joining(", ")),
					tickets.size(), booking.getTotalPrice(), statusLabel(booking), statusCss(booking));
		}).toList();

		return new MyTripsView(selected, byGroup.getOrDefault(TAB_UPCOMING, List.of()).size(),
				byGroup.getOrDefault(TAB_PAST, List.of()).size(),
				byGroup.getOrDefault(TAB_CANCELLED, List.of()).size(), cards);
	}

	/** Throws BookingNotFoundException for a booking that is missing or not the customer's. */
	@Transactional(readOnly = true)
	public BookingDetail detail(long customerId, long bookingId, LocalDateTime now) {
		Booking booking = bookingRepository.findOwnedById(bookingId, customerId)
				.orElseThrow(BookingNotFoundException::new);
		Trip trip = booking.getTrip();
		List<TicketLine> tickets = ticketRepository.findByBookingIds(List.of(bookingId)).stream()
				.map(ticket -> new TicketLine(ticket.getId(), ticket.getTicketCode(), ticket.getFullName(),
						TextUtils.maskId(ticket.getIdentityNumber()), seatLabel(ticket), ticket.getTicketPrice()))
				.toList();
		boolean cancellable = BookingCancellationService.blockReason(booking, now) == null;
		String cancelQuestion = cancellable
				? tickets.size() + " chỗ trên chuyến " + trip.getTrain().getCode() + " " + routeLabel(trip) + ", "
						+ trip.getDepartureAt().format(TIME) + " " + trip.getDepartureAt().format(DAY_MONTH)
						+ " sẽ mở bán lại. Thanh toán chỉ là mô phỏng nên không có hoàn tiền."
				: null;
		return new BookingDetail(booking.getId(), booking.getBookingCode(), routeLabel(trip), departureLabel(trip),
				arrivalLabel(trip), trip.getTrain().getCode() + " " + trip.getTrain().getName(), tickets,
				booking.getTotalPrice(), booking.getPaidAt() == null ? null : booking.getPaidAt().format(DATE_TIME),
				statusLabel(booking), statusCss(booking), cancellable, cancelQuestion);
	}

	/**
	 * The customer's cancel path: ownership first, then the shared rule.
	 * Only the id is checked here, never the booking itself: a booking read
	 * before BookingCancellationService locks it would stay in the persistence
	 * context, and the check after the lock would read that stale copy
	 * (BookingAdminService.cancel does the same).
	 *
	 * Throws BookingNotFoundException when the booking is not the customer's,
	 * BookingCancelBlockedException when it cannot be cancelled.
	 */
	@Transactional
	public Booking cancel(long customerId, long bookingId, LocalDateTime now) {
		if (!bookingRepository.existsByIdAndCustomerId(bookingId, customerId)) {
			throw new BookingNotFoundException();
		}
		return cancellationService.cancel(bookingId, now);
	}

	private static String groupOf(Booking booking, LocalDateTime now) {
		if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
			return TAB_CANCELLED;
		}
		return booking.getTrip().hasDeparted(now) ? TAB_PAST : TAB_UPCOMING;
	}

	private static String routeLabel(Trip trip) {
		return trip.getRoute().getOriginStation().getName() + " → " + trip.getRoute().getDestinationStation().getName();
	}

	private static String departureLabel(Trip trip) {
		LocalDateTime departure = trip.getDepartureAt();
		return WEEKDAYS[departure.getDayOfWeek().getValue() - 1] + ", " + departure.format(DATE) + " · "
				+ departure.format(TIME);
	}

	private static String arrivalLabel(Trip trip) {
		String time = trip.getArrivalAt().format(TIME);
		if (trip.getArrivalAt().toLocalDate().equals(trip.getDepartureAt().toLocalDate())) {
			return time;
		}
		return time + " " + trip.getArrivalAt().format(DAY_MONTH);
	}

	private static String seatLabel(Ticket ticket) {
		return "Toa " + ticket.getTripSeat().getCoachNumber() + " · " + ticket.getTripSeat().getSeatCode();
	}

	/** Bookings are created paid (D7), so a booking that is not cancelled reads as paid. */
	private static String statusLabel(Booking booking) {
		return booking.getBookingStatus() == BookingStatus.CANCELLED ? "Đã hủy" : "Đã thanh toán";
	}

	private static String statusCss(Booking booking) {
		return booking.getBookingStatus() == BookingStatus.CANCELLED ? "cancelled" : "paid";
	}

}
