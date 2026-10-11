package com.voyagego.traingo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.voyagego.traingo.service.HomeService;

@Controller
public class HomeController {

	private final HomeService homeService;

	public HomeController(HomeService homeService) {
		this.homeService = homeService;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("appName", "TrainGo");
		model.addAttribute("stations", homeService.stations());
		model.addAttribute("popularRoutes", homeService.popularRoutes());
		return "index";
	}

}
