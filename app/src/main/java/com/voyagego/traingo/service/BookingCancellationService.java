package com.voyagego.traingo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.BookingCancelBlockedException;
import com.voyagego.traingo.common.exception.BookingNotFoundException;
import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * The one place that cancels a booking, so the rule is written once: a
 * confirmed booking whose trip has not departed becomes cancelled and its
 * seats go back on sale, in one transaction. There is no fee and no refund,
 * because payment is simulated.
 *
 * It does not check who is asking. The customer's cancel path must first make
 * sure the booking is the customer's own; the admin's cancel path may cancel
 * any booking.
 */
@Service
public class BookingCancellationService {

	private static final String DEPARTED = "Không hủy được: chuyến đã khởi hành.";
	private static final String ALREADY_CANCELLED = "Booking đã hủy.";
	private static final String NOT_CONFIRMED = "Không hủy được: booking chưa được xác nhận.";

	private final BookingRepository bookingRepository;
	private final TripSeatRepository tripSeatRepository;

	public BookingCancellationService(BookingRepository bookingRepository, TripSeatRepository tripSeatRepository) {
		this.bookingRepository = bookingRepository;
		this.tripSeatRepository = tripSeatRepository;
	}

	/**
	 * Why the booking cannot be cancelled at {@code now}, or null when it can.
	 * Pages use it to explain a disabled button; {@link #cancel} uses it to refuse.
	 */
	public static String blockReason(Booking booking, LocalDateTime now) {
		if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
			return ALREADY_CANCELLED;
		}
		if (booking.getTrip().hasDeparted(now)) {
			return DEPARTED;
		}
		return booking.isCancellable(now) ? null : NOT_CONFIRMED;
	}

	/** Cancels the booking and reopens its seats. Throws BookingCancelBlockedException with the reason. */
	@Transactional
	public Booking cancel(long bookingId, LocalDateTime now) {
		Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(BookingNotFoundException::new);
		String reason = blockReason(booking, now);
		if (reason != null) {
			throw new BookingCancelBlockedException(reason);
		}
		booking.cancel();
		tripSeatRepository.releaseSeatsOfBooking(bookingId);
		return booking;
	}

}
