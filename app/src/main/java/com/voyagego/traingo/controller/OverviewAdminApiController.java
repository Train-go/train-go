package com.voyagego.traingo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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

	/**
	 * Bookings and revenue from {@code from} to {@code to} (ISO dates; the end
	 * defaults to today and the start to 29 days before it), grouped by
	 * {@code group}: day, week or month, or left out to pick one that reads well.
	 */
	@GetMapping("/sales")
	public SalesSeries sales(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			@RequestParam(required = false) String group) {
		return overviewService.sales(from, to, group, LocalDateTime.now());
	}

	/** Customers registered over the same kind of range. */
	@GetMapping("/users")
	public UsersSeries users(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			@RequestParam(required = false) String group) {
		return overviewService.users(from, to, group, LocalDateTime.now());
	}

}
