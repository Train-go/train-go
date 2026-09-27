package com.voyagego.traingo.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.voyagego.traingo.admin.service.AdminService;

/**
 * The seven admin screens of docs/product/admin.md.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	/** Shown in the header. Comes from the session once auth exists. */
	private static final String ADMIN_NAME = "Quản trị viên";

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	/**
	 * The header renders on every admin page, so the name it shows is filled in
	 * once here instead of in each handler.
	 */
	@ModelAttribute("adminName")
	String adminName() {
		return ADMIN_NAME;
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

	@GetMapping("/stations")
	public String stations(Model model) {
		model.addAttribute("activeNav", "stations");
		model.addAttribute("stations", adminService.stations());
		return "admin/stations";
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
