package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.voyagego.traingo.common.exception.BookingCancelBlockedException;
import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.service.MyTripsService;
import com.voyagego.traingo.service.TrainGoUserDetails;

/**
 * "Vé của tôi", /my-trips in docs/product/booking-flow.md. SecurityConfig lets
 * only customers in; every call passes the signed-in customer's id, and
 * MyTripsService answers someone else's booking with the 404 page.
 *
 * A cancel always redirects back to the booking (Post/Redirect/Get), with
 * the result as a flash message, so reloading never posts it again.
 */
@Controller
@RequestMapping("/my-trips")
public class MyTripsController {

	private static final String LIST_VIEW = "my-trips/list";
	private static final String DETAIL_VIEW = "my-trips/detail";

	private final MyTripsService myTripsService;

	public MyTripsController(MyTripsService myTripsService) {
		this.myTripsService = myTripsService;
	}

	/** Every page of this controller highlights "Vé của tôi" in the navbar. */
	@ModelAttribute("activeNav")
	String activeNav() {
		return "my-trips";
	}

	@GetMapping
	public String list(@RequestParam(required = false) String tab,
			@AuthenticationPrincipal TrainGoUserDetails user, Model model) {
		model.addAttribute("view", myTripsService.list(user.getUserId(), tab, LocalDateTime.now()));
		return LIST_VIEW;
	}

	@GetMapping("/{bookingId}")
	public String detail(@PathVariable long bookingId, @AuthenticationPrincipal TrainGoUserDetails user,
			Model model) {
		model.addAttribute("booking", myTripsService.detail(user.getUserId(), bookingId, LocalDateTime.now()));
		return DETAIL_VIEW;
	}

	@PostMapping("/{bookingId}/cancel")
	public String cancel(@PathVariable long bookingId, @AuthenticationPrincipal TrainGoUserDetails user,
			RedirectAttributes redirectAttributes) {
		try {
			Booking booking = myTripsService.cancel(user.getUserId(), bookingId, LocalDateTime.now());
			redirectAttributes.addFlashAttribute("successMessage", "Đã hủy booking " + booking.getBookingCode() + ".");
		} catch (BookingCancelBlockedException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/my-trips/" + bookingId;
	}

}
