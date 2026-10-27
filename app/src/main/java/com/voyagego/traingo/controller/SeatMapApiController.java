package com.voyagego.traingo.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.response.SeatMap;
import com.voyagego.traingo.service.SeatMapService;

/**
 * Seat state for the seat page. Anyone may read it: it shows which seats are
 * free and nothing about who booked them.
 */
@RestController
@RequestMapping("/api/trips")
public class SeatMapApiController {

	private final SeatMapService seatMapService;

	public SeatMapApiController(SeatMapService seatMapService) {
		this.seatMapService = seatMapService;
	}

	@GetMapping("/{tripId}/seat-map")
	public SeatMap seatMap(@PathVariable long tripId) {
		return seatMapService.seatMap(tripId, LocalDateTime.now());
	}

}
