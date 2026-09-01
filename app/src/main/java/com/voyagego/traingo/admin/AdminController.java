package com.voyagego.traingo.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The seven admin screens of docs/product/admin.md.
 *
 * Every screen is still static markup over the placeholder data declared below.
 * Nothing here touches the database: the entities arrive in stage 2 of
 * docs/plans/active/mvp-traingo.md, and each list is marked with the repository
 * that will replace it. Coaches have no route on purpose — they are managed
 * inside a train's detail page.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

	/** Shown in the header. Comes from the session once auth exists. */
	private static final String ADMIN_NAME = "Quản trị viên";

	// Replace with StationRepository.findAll().
	private static final List<Station> STATIONS = List.of(new Station("SGN", "Ga Sài Gòn", "TP. Hồ Chí Minh", true),
			new Station("NTR", "Ga Nha Trang", "Khánh Hòa", true), new Station("QNH", "Ga Quy Nhơn", "Gia Lai", true),
			new Station("DNG", "Ga Đà Nẵng", "Đà Nẵng", true), new Station("HUE", "Ga Huế", "Huế", true),
			new Station("VIN", "Ga Vinh", "Nghệ An", true), new Station("HNI", "Ga Hà Nội", "Hà Nội", true),
			new Station("LCI", "Ga Lào Cai", "Lào Cai", false));

	// Replace with RouteRepository.findAll().
	private static final List<Route> ROUTES = List.of(new Route("SGN-NTR", "Ga Sài Gòn", "Ga Nha Trang", 411),
			new Route("SGN-DNG", "Ga Sài Gòn", "Ga Đà Nẵng", 935), new Route("HNI-DNG", "Ga Hà Nội", "Ga Đà Nẵng", 791),
			new Route("HNI-LCI", "Ga Hà Nội", "Ga Lào Cai", 296),
			new Route("HNI-SGN", "Ga Hà Nội", "Ga Sài Gòn", 1726));

	// Replace with TrainRepository.findAll().
	private static final List<Train> TRAINS = List.of(new Train("SE1", "Thống Nhất", 12, 560),
			new Train("SE2", "Thống Nhất", 12, 560), new Train("SE7", "Thống Nhất", 10, 448),
			new Train("SNT1", "Sài Gòn - Nha Trang", 8, 336), new Train("SP3", "Hà Nội - Lào Cai", 9, 372));

	// Replace with TripRepository.findAll().
	private static final List<Trip> TRIPS = List.of(
			new Trip("SE1", "Ga Hà Nội — Ga Sài Gòn", "01/09/2026", "19:30", "05:10", 1_150_000, "Mở bán"),
			new Trip("SE2", "Ga Sài Gòn — Ga Hà Nội", "01/09/2026", "20:00", "05:45", 1_150_000, "Mở bán"),
			new Trip("SNT1", "Ga Sài Gòn — Ga Nha Trang", "02/09/2026", "21:15", "05:00", 420_000, "Mở bán"),
			new Trip("SP3", "Ga Hà Nội — Ga Lào Cai", "02/09/2026", "22:00", "06:05", 380_000, "Đã khởi hành"),
			new Trip("SE7", "Ga Hà Nội — Ga Đà Nẵng", "03/09/2026", "06:00", "21:40", 780_000, "Mở bán"));

	// Replace with BookingRepository.findAll().
	private static final List<Booking> BOOKINGS = List.of(
			new Booking("TG26080001", "Nguyễn Văn An", "SE1 · 01/09/2026", 2, 2_300_000, "Đã thanh toán"),
			new Booking("TG26080002", "Trần Thị Bình", "SNT1 · 02/09/2026", 1, 420_000, "Chờ thanh toán"),
			new Booking("TG26080003", "Lê Hoàng Cường", "SE2 · 01/09/2026", 4, 4_600_000, "Đã thanh toán"),
			new Booking("TG26080004", "Phạm Thu Dung", "SE7 · 03/09/2026", 2, 1_560_000, "Đã hủy"),
			new Booking("TG26080005", "Vũ Minh Đức", "SP3 · 02/09/2026", 3, 1_140_000, "Đã thanh toán"));

	// Replace with UserRepository.findAll(). Read-only screen: admin.md gives
	// this list no create, edit or delete.
	private static final List<User> USERS = List.of(
			new User("Quản trị viên", "admin@traingo.vn", "ADMIN", "01/08/2026"),
			new User("Nguyễn Văn An", "an.nguyen@example.com", "CUSTOMER", "12/08/2026"),
			new User("Trần Thị Bình", "binh.tran@example.com", "CUSTOMER", "15/08/2026"),
			new User("Lê Hoàng Cường", "cuong.le@example.com", "CUSTOMER", "18/08/2026"),
			new User("Phạm Thu Dung", "dung.pham@example.com", "CUSTOMER", "20/08/2026"));

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
		model.addAttribute("totalBookings", BOOKINGS.size());
		model.addAttribute("totalTrips", TRIPS.size());
		model.addAttribute("totalUsers", USERS.size());
		model.addAttribute("totalRevenue", 9_650_000L);
		model.addAttribute("recentBookings", BOOKINGS);
		return "admin/dashboard";
	}

	@GetMapping("/stations")
	public String stations(Model model) {
		model.addAttribute("activeNav", "stations");
		model.addAttribute("stations", STATIONS);
		return "admin/stations";
	}

	@GetMapping("/routes")
	public String routes(Model model) {
		model.addAttribute("activeNav", "routes");
		model.addAttribute("routes", ROUTES);
		return "admin/routes";
	}

	@GetMapping("/trains")
	public String trains(Model model) {
		model.addAttribute("activeNav", "trains");
		model.addAttribute("trains", TRAINS);
		return "admin/trains";
	}

	@GetMapping("/trips")
	public String trips(Model model) {
		model.addAttribute("activeNav", "trips");
		model.addAttribute("trips", TRIPS);
		return "admin/trips";
	}

	@GetMapping("/bookings")
	public String bookings(Model model) {
		model.addAttribute("activeNav", "bookings");
		model.addAttribute("bookings", BOOKINGS);
		return "admin/bookings";
	}

	@GetMapping("/users")
	public String users(Model model) {
		model.addAttribute("activeNav", "users");
		model.addAttribute("users", USERS);
		return "admin/users";
	}

	public record Station(String code, String name, String city, boolean active) {
	}

	public record Route(String code, String origin, String destination, int distanceKm) {
	}

	public record Train(String code, String name, int coaches, int seats) {
	}

	public record Trip(String trainCode, String route, String departureDate, String departureTime, String arrivalTime,
			long basePrice, String status) {
	}

	public record Booking(String code, String customer, String trip, int passengers, long total, String paymentStatus) {
	}

	public record User(String name, String email, String role, String createdAt) {
	}

}
