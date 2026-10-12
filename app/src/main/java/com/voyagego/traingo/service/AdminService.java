package com.voyagego.traingo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.PaymentStatus;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.dto.TrainCapacity;
import com.voyagego.traingo.model.response.AdminBooking;
import com.voyagego.traingo.model.response.AdminRoute;
import com.voyagego.traingo.model.response.AdminTrain;
import com.voyagego.traingo.model.response.AdminTrip;
import com.voyagego.traingo.model.response.AdminUser;
import com.voyagego.traingo.repository.BookingPassengerRepository;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.CoachRepository;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * Read-only data for the admin screens that have no controller of their own
 * yet. Everything comes from the database and is turned into the display rows
 * of {@code model/response} here, inside the transaction, because
 * {@code spring.jpa.open-in-view=false} leaves lazy relations unreadable in a
 * template. A screen leaves this class when it gets its own service, as
 * stations did. Coaches have no screen of their own on purpose: they are
 * managed inside a train's detail page.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private final RouteRepository routeRepository;
	private final TrainRepository trainRepository;
	private final CoachRepository coachRepository;
	private final TripRepository tripRepository;
	private final BookingRepository bookingRepository;
	private final BookingPassengerRepository bookingPassengerRepository;
	private final UserRepository userRepository;

	public AdminService(RouteRepository routeRepository, TrainRepository trainRepository,
			CoachRepository coachRepository, TripRepository tripRepository, BookingRepository bookingRepository,
			BookingPassengerRepository bookingPassengerRepository, UserRepository userRepository) {
		this.routeRepository = routeRepository;
		this.trainRepository = trainRepository;
		this.coachRepository = coachRepository;
		this.tripRepository = tripRepository;
		this.bookingRepository = bookingRepository;
		this.bookingPassengerRepository = bookingPassengerRepository;
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
		Map<Long, TrainCapacity> capacityByTrain = coachRepository.summarizeByTrain().stream()
				.collect(Collectors.toMap(TrainCapacity::getTrainId, Function.identity()));
		return trainRepository.findAllByOrderByCodeAsc().stream().map(train -> {
			TrainCapacity capacity = capacityByTrain.get(train.getId());
			// A train with no coach yet is listed with zero, not left out.
			int coaches = capacity == null ? 0 : (int) capacity.getCoachCount();
			int seats = capacity == null ? 0 : (int) capacity.getSeatCount();
			return new AdminTrain(train.getCode(), train.getName(), coaches, seats);
		}).toList();
	}

	public List<AdminTrip> trips() {
		return tripRepository.findAllByOrderByDepartureDateAscDepartureTimeAsc().stream()
				.map(trip -> new AdminTrip(trip.getTrain().getCode(),
						trip.getRoute().getOriginStation().getName() + " — "
								+ trip.getRoute().getDestinationStation().getName(),
						trip.getDepartureDate().format(DATE), trip.getDepartureTime().format(TIME),
						trip.getArrivalTime().format(TIME), trip.getBasePrice(), tripStatusLabel(trip)))
				.toList();
	}

	public List<AdminBooking> bookings() {
		return toRows(bookingRepository.findAllByOrderByCreatedAtDesc());
	}

	/** The five newest bookings, for the dashboard. */
	public List<AdminBooking> recentBookings() {
		return toRows(bookingRepository.findTop5ByOrderByCreatedAtDesc());
	}

	public List<AdminUser> users() {
		return userRepository.findAllByOrderByCreatedAtAsc().stream()
				.map(user -> new AdminUser(user.getFullName(), user.getEmail(), user.getRole().name(),
						user.getCreatedAt().format(DATE)))
				.toList();
	}

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
		Map<Long, Long> passengersByBooking = bookingPassengerRepository.countByBooking().stream()
				.collect(Collectors.toMap(IdCount::getId, IdCount::getTotal));
		return bookings.stream()
				.map(booking -> new AdminBooking(booking.getBookingCode(), booking.getCustomer().getFullName(),
						booking.getTrip().getTrain().getCode() + " · " + booking.getTrip().getDepartureDate().format(DATE),
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

	private static String tripStatusLabel(Trip trip) {
		return switch (trip.getStatus()) {
			case SCHEDULED -> "Mở bán";
			case COMPLETED -> "Đã khởi hành";
			case CANCELLED -> "Đã hủy";
		};
	}

}
