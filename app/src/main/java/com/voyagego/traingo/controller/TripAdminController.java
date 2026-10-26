package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.voyagego.traingo.service.TripAdminService;

/**
 * /admin/trips in docs/product/admin.md: every trip, to look one up and open
 * its sheet. The filters live in the URL (?tab=&train=&q=) so a filtered list
 * can be reloaded and shared. Trips are created on their train's page, and
 * the sheet cancels through TripAdminApiController.
 */
@Controller
@RequestMapping("/admin/trips")
public class TripAdminController {

	private static final String LIST_VIEW = "admin/trips";

	private final TripAdminService tripAdminService;

	public TripAdminController(TripAdminService tripAdminService) {
		this.tripAdminService = tripAdminService;
	}

	/** Every page of this controller highlights the same sidebar entry. */
	@ModelAttribute("activeNav")
	String activeNav() {
		return "trips";
	}

	@GetMapping
	public String list(@RequestParam(required = false) String tab, @RequestParam(required = false) Long train,
			@RequestParam(required = false) String q, Model model) {
		model.addAttribute("view", tripAdminService.list(tab, train, q, LocalDateTime.now()));
		return LIST_VIEW;
	}

}
