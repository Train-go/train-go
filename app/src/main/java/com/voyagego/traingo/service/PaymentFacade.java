package com.voyagego.traingo.service;

import java.time.LocalDateTime;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.model.dto.BookingDraft;

/**
 * Pays a draft, and pays again when two payments of the same month built the
 * same booking code at the same time. It has no transaction of its own on
 * purpose: each call to {@link PaymentService#pay} is one transaction, so a
 * repeat starts fresh, reads the code the other payment saved, and the seats
 * the failed try booked were already rolled back.
 */
@Component
public class PaymentFacade {

	static final int ATTEMPTS = 3;
	private static final String CODE_KEY = "uk_bookings_code";

	private final PaymentService paymentService;

	public PaymentFacade(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	public Booking pay(BookingDraft draft, long customerId, LocalDateTime now) {
		for (int attempt = 1;; attempt++) {
			try {
				return paymentService.pay(draft, customerId, now);
			} catch (DataIntegrityViolationException ex) {
				String cause = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
				if (attempt >= ATTEMPTS || cause == null || !cause.contains(CODE_KEY)) {
					throw ex;
				}
			}
		}
	}

}
