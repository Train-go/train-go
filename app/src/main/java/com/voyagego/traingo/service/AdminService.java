package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.PaymentStatus;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.response.AdminBooking;
import com.voyagego.traingo.model.response.AdminRoute;
import com.voyagego.traingo.model.response.AdminTrain;
import com.voyagego.traingo.model.response.AdminTrip;
import com.voyagego.traingo.model.response.AdminUser;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.TicketRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * Read-only data for the admin screens that have no controller of their own
 * yet. Everything comes from the database and is turned into the display rows
 * of {@code model/response} here, inside the transaction, because
 * {@code spring.jpa.open-in-view=false} leaves lazy relations unreadable in a
 * template. A screen leaves this class when it gets its own service, as
 * stations did.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

	private final RouteRepository routeRepository;
	private final TrainRepository trainRepository;
	private final TripRepository tripRepository;
	private final BookingRepository bookingRepository;
	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;

	public AdminService(RouteRepository routeRepository, TrainRepository trainRepository,
			TripRepository tripRepository, BookingRepository bookingRepository,
			TicketRepository ticketRepository, UserRepository userRepository) {
		this.routeRepository = routeRepository;
		this.trainRepository = trainRepository;
		this.tripRepository = tripRepository;
		this.bookingRepository = bookingRepository;
		this.ticketRepository = ticketRepository;
		this.userRepository = userRepository;
	}

	public List<AdminRoute> routes() {
		return routeRepository.findAllWithStations().stream()
				.map(route -> new AdminRoute(
						route.getOriginStation().getCode() + "-" + route.getDestinationStation().getCode(),
						route.getOriginStation().getName(), route.getDestinationStation().getName(),
						route.getDistanceKm()))
				.toList();
	}

	public List<AdminTrain> trains() {
		return trainRepository.findAllByOrderByCodeAsc().stream()
				.map(train -> new AdminTrain(train.getCode(), train.getName())).toList();
	}

	public List<AdminTrip> trips() {
		LocalDateTime now = LocalDateTime.now();
		return tripRepository.findAllByOrderByDepartureAtAsc().stream()
				.map(trip -> new AdminTrip(trip.getTrain().getCode(),
						trip.getRoute().getOriginStation().getName() + " → "
								+ trip.getRoute().getDestinationStation().getName(),
						trip.getDepartureAt().format(DATE), trip.getDepartureAt().format(TIME),
						arrivalText(trip), trip.getBasePrice(), tripStatusLabel(trip, now)))
				.toList();
	}

	public List<AdminBooking> bookings() {
		return toRows(bookingRepository.findAllByOrderByCreatedAtDesc());
	}

	public List<AdminUser> users() {
		return userRepository.findAllByOrderByCreatedAtAsc().stream()
				.map(user -> new AdminUser(user.getFullName(), user.getEmail(), user.getRole().name(),
						user.getCreatedAt().format(DATE)))
				.toList();
	}

	/*
	 * The four totals below have no page while /admin redirects to the trains
	 * screen. They stay for the overview page (#37), which builds on them.
	 */

	public long totalBookings() {
		return bookingRepository.count();
	}

	public long totalTrips() {
		return tripRepository.count();
	}

	public long totalUsers() {
		return userRepository.count();
	}

	/**
	 * Money from confirmed bookings only. A cancelled booking stays PAID in the
	 * database, but the project has no refund flow and does not count it.
	 */
	public long totalRevenue() {
		return bookingRepository.sumTotalPriceByBookingStatus(BookingStatus.CONFIRMED);
	}

	private List<AdminBooking> toRows(List<Booking> bookings) {
		Map<Long, Long> passengersByBooking = ticketRepository.countByBooking().stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		return bookings.stream()
				.map(booking -> new AdminBooking(booking.getBookingCode(), booking.getCustomer().getFullName(),
						booking.getTrip().getTrain().getCode() + " · " + booking.getTrip().getDepartureAt().format(DATE),
						passengersByBooking.getOrDefault(booking.getId(), 0L).intValue(), booking.getTotalPrice(),
						bookingLabel(booking)))
				.toList();
	}

	/**
	 * The admin tables show one status column per booking. A cancelled booking
	 * reads as cancelled whatever its payment status says.
	 */
	private static String bookingLabel(Booking booking) {
		if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
			return "Đã hủy";
		}
		return booking.getPaymentStatus() == PaymentStatus.PAID ? "Đã thanh toán" : "Chờ thanh toán";
	}

	/** The arrival time, with its date when the trip arrives on a later day. */
	private static String arrivalText(Trip trip) {
		String time = trip.getArrivalAt().format(TIME);
		if (trip.getArrivalAt().toLocalDate().equals(trip.getDepartureAt().toLocalDate())) {
			return time;
		}
		return time + " " + trip.getArrivalAt().format(DAY_MONTH);
	}

	/**
	 * A trip counts as departed once its departure time has passed, whether or
	 * not anyone has marked it COMPLETED yet (D8 in chia-viec-mvp.md).
	 */
	private static String tripStatusLabel(Trip trip, LocalDateTime now) {
		return switch (trip.getStatus()) {
			case SCHEDULED -> trip.getDepartureAt().isAfter(now) ? "Mở bán" : "Đã khởi hành";
			case COMPLETED -> "Đã khởi hành";
			case CANCELLED -> "Đã hủy";
		};
	}

}
