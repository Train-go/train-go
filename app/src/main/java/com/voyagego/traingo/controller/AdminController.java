package com.voyagego.traingo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The admin area's entry point. Every admin screen has its own controller
 * now; /admin/trips moved to TripAdminController.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	/**
	 * The admin mockup has no overview page, so /admin opens the trains
	 * screen (TrainAdminController) until the overview task (#37) brings one
	 * back.
	 */
	@GetMapping
	public String home() {
		return "redirect:/admin/trains";
	}

}
