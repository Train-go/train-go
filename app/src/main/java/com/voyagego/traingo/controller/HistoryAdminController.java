package com.voyagego.traingo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.service.TrainService;

/**
 * "Lịch sử", the last entry of the admin menu: records the admin removed from
 * the working screens. For now the soft-deleted trains, each with a restore
 * button (static/js/admin/history.js).
 */
@Controller
@RequestMapping("/admin/history")
public class HistoryAdminController {

	private final TrainService trainService;

	public HistoryAdminController(TrainService trainService) {
		this.trainService = trainService;
	}

	@GetMapping
	public String history(Model model) {
		model.addAttribute("activeNav", "history");
		model.addAttribute("deletedTrains", trainService.findDeleted());
		return "admin/history";
	}

}
