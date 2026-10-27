package com.voyagego.traingo.controller;

import java.util.List;
import java.util.Map;

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

import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.model.request.RouteBatchRequest;
import com.voyagego.traingo.model.request.RouteCreateRequest;
import com.voyagego.traingo.model.request.StationRequest;
import com.voyagego.traingo.model.response.NetworkView;
import com.voyagego.traingo.model.response.NetworkView.StationNode;
import com.voyagego.traingo.service.NetworkService;
import com.voyagego.traingo.service.RouteService;
import com.voyagego.traingo.service.StationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class NetworkAdminApiController {

	private final NetworkService networkService;
	private final StationService stationService;
	private final RouteService routeService;

	public NetworkAdminApiController(NetworkService networkService, StationService stationService,
			RouteService routeService) {
		this.networkService = networkService;
		this.stationService = stationService;
		this.routeService = routeService;
	}

	@GetMapping("/network")
	public NetworkView network() {
		return networkService.network();
	}

	@PostMapping("/stations")
	@ResponseStatus(HttpStatus.CREATED)
	public StationNode createStation(@Valid @RequestBody StationRequest request) {
		return networkService.stationNode(stationService.create(request));
	}

	@PutMapping("/stations/{id}")
	public StationNode updateStation(@PathVariable long id, @Valid @RequestBody StationRequest request) {
		return networkService.stationNode(stationService.update(id, request));
	}

	@DeleteMapping("/stations/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteStation(@PathVariable long id) {
		stationService.delete(id);
	}

	@GetMapping("/stations/free-km")
	public Map<String, Integer> freeKm(@RequestParam int lo, @RequestParam int hi) {
		return Map.of("km", stationService.freeKmBetween(lo, hi));
	}

	@PostMapping("/routes")
	@ResponseStatus(HttpStatus.CREATED)
	public List<Long> createRoutes(@Valid @RequestBody RouteCreateRequest request) {
		return routeService.create(request).stream().map(Route::getId).toList();
	}

	/**
	 * Creates several routes in one transaction (#38, #39). A direction that
	 * already exists is skipped, so {@code ids} lists only the routes created.
	 */
	@PostMapping("/routes/batch")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, List<Long>> createRoutesBatch(@Valid @RequestBody RouteBatchRequest request) {
		return Map.of("ids", routeService.createMany(request).stream().map(Route::getId).toList());
	}

	/** "Hoàn tác" of a batch: deletes the routes in {@code ids}, all or none. */
	@DeleteMapping("/routes/batch")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteRoutesBatch(@RequestParam List<Long> ids) {
		routeService.deleteMany(ids);
	}

	@DeleteMapping("/routes/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteRoute(@PathVariable long id) {
		routeService.delete(id);
	}

}
