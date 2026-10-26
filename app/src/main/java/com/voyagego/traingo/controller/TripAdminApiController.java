package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.response.TripDetail;
import com.voyagego.traingo.service.TripAdminService;

/**
 * JSON behind the trip sheet that any admin page can open through
 * TG.openTrip (static/js/admin/trip-sheet.js). An unknown id is a 404
 * ApiError (ApiExceptionHandler).
 */
@RestController
@RequestMapping("/api/admin/trips")
public class TripAdminApiController {

	private final TripAdminService tripAdminService;

	public TripAdminApiController(TripAdminService tripAdminService) {
		this.tripAdminService = tripAdminService;
	}

	@GetMapping("/{id}")
	public TripDetail detail(@PathVariable long id) {
		return tripAdminService.detail(id, LocalDateTime.now());
	}

}
