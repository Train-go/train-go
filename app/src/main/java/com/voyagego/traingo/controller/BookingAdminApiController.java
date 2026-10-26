package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.response.BookingAdminDetail;
import com.voyagego.traingo.service.BookingAdminService;

/**
 * JSON behind the Booking page's detail sheet, called by
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

}
