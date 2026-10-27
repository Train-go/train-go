package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.response.TrainScheduleView;
import com.voyagego.traingo.service.TrainScheduleService;
import com.voyagego.traingo.service.TrainService;

/**
 * The train page (docs/product/admin.md, "Quản lý tàu"): one tab per train,
 * then the open train's position and schedule, in the view "Theo ngày" or, with
 * ?view=bang, "Bảng tính". Adding and
 * editing a train go through TrainAdminApiController from the page's sheet.
 */
@Controller
@RequestMapping("/admin/trains")
public class TrainAdminController {

	private static final String VIEW = "admin/train";
	private static final String SHEET_VIEW = "bang";

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
	public String first(@RequestParam(required = false) String view, Model model) {
		List<Train> trains = trainService.findAll();
		if (trains.isEmpty()) {
			model.addAttribute("trains", trains);
			return VIEW;
		}
		// The view the browser remembered goes along, or the next redirect would drop it.
		return "redirect:/admin/trains/" + trains.get(0).getId() + (SHEET_VIEW.equals(view) ? "?view=bang" : "");
	}

	@GetMapping("/{id}")
	public String show(@PathVariable long id, @RequestParam(required = false) String view, Model model) {
		LocalDateTime now = LocalDateTime.now();
		// Throws TrainNotFoundException, shown as the 404 page, for an unknown
		// or deleted train.
		TrainScheduleView schedule = trainScheduleService.scheduleOf(id, now);
		// "bang" is the spreadsheet view; anything else is the day view.
		model.addAttribute("view", SHEET_VIEW.equals(view) ? SHEET_VIEW : "ngay");
		model.addAttribute("schedule", schedule);
		model.addAttribute("train", schedule.train());
		model.addAttribute("trains", trainService.findAll());
		// Counts cancelled trips too, unlike the schedule's chain end.
		model.addAttribute("hasTrips", trainService.hasTrips(id));
		// "Xóa tàu" only shows when TrainService.delete would accept it.
		model.addAttribute("canDelete", trainService.countUnfinishedTrips(id, now) == 0);
		return VIEW;
	}

	/**
	 * The trip wizard: one leg after the train's last trip, in four steps. The
	 * page loads its choices from GET /api/admin/trains/{id}/next-leg and adds
	 * the leg through POST /api/admin/trains/{id}/trips.
	 */
	@GetMapping("/{id}/trips/new")
	public String newTrip(@PathVariable long id, Model model) {
		// Throws TrainNotFoundException, shown as the 404 page, for an unknown
		// or deleted train.
		model.addAttribute("train", trainService.findById(id));
		model.addAttribute("chained", trainScheduleService.chainEnd(id).isPresent());
		model.addAttribute("seatsPerTrip", CoachLayout.totalSeats());
		return "admin/trip-wizard";
	}

}
