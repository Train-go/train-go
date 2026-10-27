package com.voyagego.traingo.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.voyagego.traingo.repository.BookingRepository;

/**
 * Booking and ticket codes (D5): "BK" + year and month + a three-digit number
 * that continues the greatest one of the month, for example BK202610005, and
 * the ticket codes TK202610005-01, -02 ...
 *
 * Two payments in the same month can read the same greatest code and build the
 * same next one. The unique key uk_bookings_code refuses the second; PaymentFacade
 * then pays again, and the new transaction reads the code the first one saved.
 */
@Component
public class BookingCodeGenerator {

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM", Locale.ROOT);
	private static final int MAX_PER_MONTH = 999;

	private final BookingRepository bookingRepository;

	public BookingCodeGenerator(BookingRepository bookingRepository) {
		this.bookingRepository = bookingRepository;
	}

	public String nextBookingCode(YearMonth month) {
		String prefix = "BK" + month.format(MONTH);
		int last = bookingRepository.findMaxCodeWithPrefix(prefix)
				.map(code -> Integer.parseInt(code.substring(prefix.length())))
				.orElse(0);
		if (last >= MAX_PER_MONTH) {
			throw new IllegalStateException("Month " + month + " already has " + MAX_PER_MONTH + " bookings");
		}
		return String.format(Locale.ROOT, "%s%03d", prefix, last + 1);
	}

	/** TK202610005-01 for the first passenger of BK202610005. */
	public static String ticketCode(String bookingCode, int index) {
		return String.format(Locale.ROOT, "TK%s-%02d", bookingCode.substring(2), index);
	}

}
