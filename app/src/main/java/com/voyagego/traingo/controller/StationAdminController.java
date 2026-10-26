package com.voyagego.traingo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.service.StationService;

@Controller
@RequestMapping("/admin/stations")
public class StationAdminController {

	private final StationService stationService;

	public StationAdminController(StationService stationService) {
		this.stationService = stationService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("activeNav", "stations");
		model.addAttribute("stations", stationService.findAll());
		return "admin/stations";
	}

}
