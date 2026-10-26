package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.voyagego.traingo.service.BookingAdminService;

/**
 * The Booking page (docs/product/admin.md, "Quản lý booking"): three groups,
 * a search box and, with ?customer=, one customer's bookings. A booking's
 * detail sheet is filled by BookingAdminApiController; ?open= only tells
 * bookings.js which one to open on load.
 */
@Controller
@RequestMapping("/admin/bookings")
public class BookingAdminController {

	private final BookingAdminService bookingAdminService;

	public BookingAdminController(BookingAdminService bookingAdminService) {
		this.bookingAdminService = bookingAdminService;
	}

	@ModelAttribute("activeNav")
	String activeNav() {
		return "bookings";
	}

	@GetMapping
	public String list(@RequestParam(required = false) String tab, @RequestParam(required = false) String q,
			@RequestParam(name = "customer", required = false) Long customerId,
			@RequestParam(required = false) String open, Model model) {
		model.addAttribute("view", bookingAdminService.list(tab, q, customerId, LocalDateTime.now()));
		model.addAttribute("open", open);
		return "admin/bookings";
	}

}
