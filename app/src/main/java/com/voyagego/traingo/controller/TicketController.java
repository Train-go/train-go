package com.voyagego.traingo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.voyagego.traingo.service.TicketService;
import com.voyagego.traingo.service.TrainGoUserDetails;

/**
 * The e-tickets (issue #34): one ticket at /tickets/{id} and every ticket of a
 * booking at /my-trips/{id}/tickets. SecurityConfig lets only customers in to
 * both paths; TicketService answers someone else's ticket with the 404 page.
 */
@Controller
public class TicketController {

	private final TicketService ticketService;

	public TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	/** Both pages highlight "Vé của tôi" in the navbar. */
	@ModelAttribute("activeNav")
	String activeNav() {
		return "my-trips";
	}

	@GetMapping("/tickets/{ticketId}")
	public String ticket(@PathVariable long ticketId, @AuthenticationPrincipal TrainGoUserDetails user, Model model) {
		model.addAttribute("ticket", ticketService.ticket(user.getUserId(), ticketId));
		return "tickets/ticket";
	}

	@GetMapping("/my-trips/{bookingId}/tickets")
	public String bookingTickets(@PathVariable long bookingId, @AuthenticationPrincipal TrainGoUserDetails user,
			Model model) {
		var tickets = ticketService.ticketsOfBooking(user.getUserId(), bookingId);
		model.addAttribute("bookingId", bookingId);
		model.addAttribute("tickets", tickets);
		model.addAttribute("bookingCode", tickets.isEmpty() ? null : tickets.get(0).bookingCode());
		return "tickets/booking-tickets";
	}

}
