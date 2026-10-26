package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.request.TrainRequest;
import com.voyagego.traingo.model.response.PlannedLeg;
import com.voyagego.traingo.service.TrainScheduleService;
import com.voyagego.traingo.service.TrainScheduleService.Pace;
import com.voyagego.traingo.service.TrainService;

import jakarta.validation.Valid;

/**
 * JSON endpoints behind the trains page, called by static/js/admin/train.js
 * through TG.api: add, edit, soft delete, restore and plan. Errors take the ApiError
 * shape of ApiExceptionHandler: 400 with one message per field, 404 for a
 * missing train, 409 for a broken rule (duplicate or locked code, unfinished
 * trips, code taken by another train).
 */
@RestController
@RequestMapping("/api/admin/trains")
public class TrainAdminApiController {

	private final TrainService trainService;
	private final TrainScheduleService trainScheduleService;

	public TrainAdminApiController(TrainService trainService, TrainScheduleService trainScheduleService) {
		this.trainService = trainService;
		this.trainScheduleService = trainScheduleService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> create(@Valid @RequestBody TrainRequest form) {
		return Map.of("id", trainService.create(form).getId());
	}

	@PutMapping("/{id}")
	public Map<String, Long> update(@PathVariable long id, @Valid @RequestBody TrainRequest form) {
		return Map.of("id", trainService.update(id, form).getId());
	}

	@DeleteMapping("/{id}")
	public Map<String, Long> delete(@PathVariable long id) {
		return Map.of("id", trainService.delete(id, LocalDateTime.now()).getId());
	}

	@PostMapping("/{id}/restore")
	public Map<String, Long> restore(@PathVariable long id) {
		return Map.of("id", trainService.restore(id).getId());
	}

	/**
	 * A multi-leg plan from one station to another along neighbouring
	 * stations, for "Điền nhanh" in the sheet view and the wizard's multi-leg
	 * mode. 409 when a pair of neighbouring stations has no route.
	 */
	@GetMapping("/{id}/plan")
	public List<PlannedLeg> plan(@PathVariable long id, @RequestParam long from, @RequestParam long to,
			@RequestParam Pace pace, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime firstDeparture) {
		return trainScheduleService.planLegs(id, from, to, pace, firstDeparture);
	}

}
