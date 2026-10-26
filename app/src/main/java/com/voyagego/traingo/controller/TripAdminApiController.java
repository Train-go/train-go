package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.response.TripDetail;
import com.voyagego.traingo.service.TripAdminService;

/**
 * JSON for the trip sheet (static/js/admin/trip-sheet.js), opened from the
 * trains and trips pages. Errors are not caught here: ApiExceptionHandler
 * answers a missing trip with 404 and a trip that cannot be cancelled with
 * 409, both as ApiError.
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

	@PostMapping("/{id}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable long id) {
		tripAdminService.cancel(id, LocalDateTime.now());
	}

}
