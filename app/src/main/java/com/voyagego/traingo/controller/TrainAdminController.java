package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.model.response.TrainScheduleView;
import com.voyagego.traingo.service.TrainScheduleService;
import com.voyagego.traingo.service.TrainService;

/**
 * The train page, view "Theo ngày" (docs/product/admin.md, "Quản lý tàu"):
 * one tab per train, then the open train's position and schedule. Adding and
 * editing a train go through TrainAdminApiController from the page's sheet.
 */
@Controller
@RequestMapping("/admin/trains")
public class TrainAdminController {

	private static final String VIEW = "admin/train";

	private final TrainService trainService;
	private final TrainScheduleService trainScheduleService;

	public TrainAdminController(TrainService trainService, TrainScheduleService trainScheduleService) {
		this.trainService = trainService;
		this.trainScheduleService = trainScheduleService;
	}

	@ModelAttribute("activeNav")
	String activeNav() {
		return "trains";
	}

	/**
	 * Opens the first train by code. With no train in use the page only offers
	 * "Thêm tàu"; deleted trains live under /admin/history.
	 */
	@GetMapping
	public String first(Model model) {
		List<Train> trains = trainService.findAll();
		if (trains.isEmpty()) {
			model.addAttribute("trains", trains);
			return VIEW;
		}
		return "redirect:/admin/trains/" + trains.get(0).getId();
	}

	@GetMapping("/{id}")
	public String show(@PathVariable long id, Model model) {
		LocalDateTime now = LocalDateTime.now();
		// Throws TrainNotFoundException, shown as the 404 page, for an unknown
		// or deleted train.
		TrainScheduleView schedule = trainScheduleService.scheduleOf(id, now);
		model.addAttribute("schedule", schedule);
		model.addAttribute("train", schedule.train());
		model.addAttribute("trains", trainService.findAll());
		// Counts cancelled trips too, unlike the schedule's chain end.
		model.addAttribute("hasTrips", trainService.hasTrips(id));
		// "Xóa tàu" only shows when TrainService.delete would accept it.
		model.addAttribute("canDelete", trainService.countUnfinishedTrips(id, now) == 0);
		return VIEW;
	}

}
