package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.response.BookingAdminDetail;
import com.voyagego.traingo.service.BookingAdminService;

/**
 * JSON behind the Booking page's detail sheet and its cancel button, called by
 * static/js/admin/bookings.js through TG.api. An unknown code is a 404
 * ApiError (ApiExceptionHandler).
 */
@RestController
@RequestMapping("/api/admin/bookings")
public class BookingAdminApiController {

	private final BookingAdminService bookingAdminService;

	public BookingAdminApiController(BookingAdminService bookingAdminService) {
		this.bookingAdminService = bookingAdminService;
	}

	@GetMapping("/{code}")
	public BookingAdminDetail detail(@PathVariable String code) {
		return bookingAdminService.detail(code, LocalDateTime.now());
	}

	/**
	 * Cancels the booking and puts its seats back on sale. 409 when it cannot
	 * be cancelled (the trip has departed, or it is already cancelled).
	 */
	@PostMapping("/{code}/cancel")
	public Map<String, Integer> cancel(@PathVariable String code) {
		return Map.of("releasedSeats", bookingAdminService.cancel(code, LocalDateTime.now()));
	}

}
