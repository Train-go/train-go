package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.response.SalesSeries;
import com.voyagego.traingo.model.response.UsersSeries;
import com.voyagego.traingo.service.OverviewService;

/** The two charts of the admin overview, drawn by overview.js. */
@RestController
@RequestMapping("/api/admin/overview")
public class OverviewAdminApiController {

	private final OverviewService overviewService;

	public OverviewAdminApiController(OverviewService overviewService) {
		this.overviewService = overviewService;
	}

	/** Bookings and revenue per day over the last {@code days} days: 7, 14 or 30. */
	@GetMapping("/sales")
	public SalesSeries sales(@RequestParam(defaultValue = "14") int days) {
		return overviewService.sales(days, LocalDateTime.now());
	}

	/** Customers registered per day over the last {@code days} days: 7, 14 or 30. */
	@GetMapping("/users")
	public UsersSeries users(@RequestParam(defaultValue = "14") int days) {
		return overviewService.users(days, LocalDateTime.now());
	}

}
