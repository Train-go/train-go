package com.voyagego.traingo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.repository.BookingRepository;
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

	private final TripRepository tripRepository;
	private final BookingRepository bookingRepository;
	private final UserRepository userRepository;

	public AdminService(TripRepository tripRepository, BookingRepository bookingRepository,
			UserRepository userRepository) {
		this.tripRepository = tripRepository;
		this.bookingRepository = bookingRepository;
		this.userRepository = userRepository;
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

}
