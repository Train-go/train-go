package com.voyagego.traingo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.voyagego.traingo.service.HomeService;

@Controller
public class HomeController {

	private final HomeService homeService;

	public HomeController(HomeService homeService) {
		this.homeService = homeService;
	}

	@GetMapping("/")
	public String home(@RequestParam(required = false) String region,
			@RequestParam(defaultValue = "" + HomeService.FIRST_PAGE) int limit, Model model) {
		model.addAttribute("appName", "TrainGo");
		model.addAttribute("stations", homeService.stations());
		model.addAttribute("today", LocalDate.now());
		model.addAttribute("popular", homeService.popular(region, limit, LocalDateTime.now()));
		return "index";
	}

}
