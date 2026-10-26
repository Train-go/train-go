package com.voyagego.traingo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.service.AdminService;

/**
 * The admin screens of docs/product/admin.md that are still read-only lists
 * without create, edit or delete. A screen moves out of here into its own
 * controller once it gets those actions, as /admin/stations did
 * (StationAdminController).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	/**
	 * The admin mockup has no overview page, so /admin opens the trains
	 * screen (TrainAdminController) until the overview task (#37) brings one
	 * back.
	 */
	@GetMapping
	public String home() {
		return "redirect:/admin/trains";
	}

	@GetMapping("/trips")
	public String trips(Model model) {
		model.addAttribute("activeNav", "trips");
		model.addAttribute("trips", adminService.trips());
		return "admin/trips";
	}

	@GetMapping("/bookings")
	public String bookings(Model model) {
		model.addAttribute("activeNav", "bookings");
		model.addAttribute("bookings", adminService.bookings());
		return "admin/bookings";
	}

	@GetMapping("/users")
	public String users(Model model) {
		model.addAttribute("activeNav", "users");
		model.addAttribute("users", adminService.users());
		return "admin/users";
	}

}
