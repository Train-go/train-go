package com.voyagego.traingo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.model.response.Overview;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * The numbers on /admin (#37). A cancelled booking stays PAID in the database,
 * but the project has no refund flow, so only CONFIRMED bookings count as
 * bookings and as revenue.
 */
@Service
@Transactional(readOnly = true)
public class OverviewService {

	private final BookingRepository bookingRepository;
	private final TripRepository tripRepository;
	private final UserRepository userRepository;

	public OverviewService(BookingRepository bookingRepository, TripRepository tripRepository,
			UserRepository userRepository) {
		this.bookingRepository = bookingRepository;
		this.tripRepository = tripRepository;
		this.userRepository = userRepository;
	}

	public Overview overview(LocalDateTime now) {
		return new Overview(
				bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED),
				bookingRepository.countByBookingStatus(BookingStatus.CANCELLED),
				tripRepository.countByStatusNot(TripStatus.CANCELLED),
				tripRepository.countByStatusAndDepartureAtAfter(TripStatus.SCHEDULED, now),
				userRepository.countByRole(UserRole.CUSTOMER),
				bookingRepository.sumTotalPriceByBookingStatus(BookingStatus.CONFIRMED));
	}

}
