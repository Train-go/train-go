package com.voyagego.traingo.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.admin.service.AdminService;

/**
 * The admin dashboard, plus the admin screens of docs/product/admin.md that
 * still show placeholder data. A screen moves out of here into its feature
 * package once it reads real data, as /admin/stations did
 * (station/controller/StationAdminController).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping
	public String dashboard(Model model) {
		model.addAttribute("activeNav", "dashboard");
		model.addAttribute("totalBookings", adminService.bookings().size());
		model.addAttribute("totalTrips", adminService.trips().size());
		model.addAttribute("totalUsers", adminService.users().size());
		model.addAttribute("totalRevenue", adminService.totalRevenue());
		model.addAttribute("recentBookings", adminService.bookings());
		return "admin/dashboard";
	}

	@GetMapping("/routes")
	public String routes(Model model) {
		model.addAttribute("activeNav", "routes");
		model.addAttribute("routes", adminService.routes());
		return "admin/routes";
	}

	@GetMapping("/trains")
	public String trains(Model model) {
		model.addAttribute("activeNav", "trains");
		model.addAttribute("trains", adminService.trains());
		return "admin/trains";
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
