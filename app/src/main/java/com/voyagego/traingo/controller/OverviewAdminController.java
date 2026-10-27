package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.service.OverviewService;

/**
 * The admin area's entry point, /admin: four totals read from the database
 * (#37). Every other admin screen has its own controller.
 */
@Controller
@RequestMapping("/admin")
public class OverviewAdminController {

	private final OverviewService overviewService;

	public OverviewAdminController(OverviewService overviewService) {
		this.overviewService = overviewService;
	}

	@ModelAttribute("activeNav")
	String activeNav() {
		return "overview";
	}

	@GetMapping
	public String overview(Model model) {
		model.addAttribute("overview", overviewService.overview(LocalDateTime.now()));
		return "admin/overview";
	}

}
