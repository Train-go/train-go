package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.voyagego.traingo.model.request.TrainRequest;
import com.voyagego.traingo.service.TrainService;

import jakarta.validation.Valid;

/**
 * JSON endpoints behind the trains page, called by static/js/admin/train.js
 * through TG.api: add, edit, soft delete and restore. Errors take the ApiError
 * shape of ApiExceptionHandler: 400 with one message per field, 404 for a
 * missing train, 409 for a broken rule (duplicate or locked code, unfinished
 * trips, code taken by another train).
 */
@RestController
@RequestMapping("/api/admin/trains")
public class TrainAdminApiController {

	private final TrainService trainService;

	public TrainAdminApiController(TrainService trainService) {
		this.trainService = trainService;
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

}
