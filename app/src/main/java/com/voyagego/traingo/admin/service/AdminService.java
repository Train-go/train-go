package com.voyagego.traingo.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.voyagego.traingo.admin.model.Booking;
import com.voyagego.traingo.admin.model.Route;
import com.voyagego.traingo.admin.model.Station;
import com.voyagego.traingo.admin.model.Train;
import com.voyagego.traingo.admin.model.Trip;
import com.voyagego.traingo.admin.model.User;

/**
 * Placeholder data for the seven admin screens, standing in for the
 * repositories that arrive in stage 2 of docs/plans/active/mvp-traingo.md.
 * Coaches have no route on purpose — they are managed inside a train's detail
 * page.
 */
@Service
public class AdminService {

	private static final List<Station> STATIONS = List.of(new Station("SGN", "Ga Sài Gòn", "TP. Hồ Chí Minh", true),
			new Station("NTR", "Ga Nha Trang", "Khánh Hòa", true), new Station("QNH", "Ga Quy Nhơn", "Gia Lai", true),
			new Station("DNG", "Ga Đà Nẵng", "Đà Nẵng", true), new Station("HUE", "Ga Huế", "Huế", true),
			new Station("VIN", "Ga Vinh", "Nghệ An", true), new Station("HNI", "Ga Hà Nội", "Hà Nội", true),
			new Station("LCI", "Ga Lào Cai", "Lào Cai", false));

	private static final List<Route> ROUTES = List.of(new Route("SGN-NTR", "Ga Sài Gòn", "Ga Nha Trang", 411),
			new Route("SGN-DNG", "Ga Sài Gòn", "Ga Đà Nẵng", 935), new Route("HNI-DNG", "Ga Hà Nội", "Ga Đà Nẵng", 791),
			new Route("HNI-LCI", "Ga Hà Nội", "Ga Lào Cai", 296),
			new Route("HNI-SGN", "Ga Hà Nội", "Ga Sài Gòn", 1726));

	private static final List<Train> TRAINS = List.of(new Train("SE1", "Thống Nhất", 12, 560),
			new Train("SE2", "Thống Nhất", 12, 560), new Train("SE7", "Thống Nhất", 10, 448),
			new Train("SNT1", "Sài Gòn - Nha Trang", 8, 336), new Train("SP3", "Hà Nội - Lào Cai", 9, 372));

	private static final List<Trip> TRIPS = List.of(
			new Trip("SE1", "Ga Hà Nội — Ga Sài Gòn", "01/09/2026", "19:30", "05:10", 1_150_000, "Mở bán"),
			new Trip("SE2", "Ga Sài Gòn — Ga Hà Nội", "01/09/2026", "20:00", "05:45", 1_150_000, "Mở bán"),
			new Trip("SNT1", "Ga Sài Gòn — Ga Nha Trang", "02/09/2026", "21:15", "05:00", 420_000, "Mở bán"),
			new Trip("SP3", "Ga Hà Nội — Ga Lào Cai", "02/09/2026", "22:00", "06:05", 380_000, "Đã khởi hành"),
			new Trip("SE7", "Ga Hà Nội — Ga Đà Nẵng", "03/09/2026", "06:00", "21:40", 780_000, "Mở bán"));

	private static final List<Booking> BOOKINGS = List.of(
			new Booking("TG26080001", "Nguyễn Văn An", "SE1 · 01/09/2026", 2, 2_300_000, "Đã thanh toán"),
			new Booking("TG26080002", "Trần Thị Bình", "SNT1 · 02/09/2026", 1, 420_000, "Chờ thanh toán"),
			new Booking("TG26080003", "Lê Hoàng Cường", "SE2 · 01/09/2026", 4, 4_600_000, "Đã thanh toán"),
			new Booking("TG26080004", "Phạm Thu Dung", "SE7 · 03/09/2026", 2, 1_560_000, "Đã hủy"),
			new Booking("TG26080005", "Vũ Minh Đức", "SP3 · 02/09/2026", 3, 1_140_000, "Đã thanh toán"));

	// Read-only screen: admin.md gives this list no create, edit or delete.
	private static final List<User> USERS = List.of(
			new User("Quản trị viên", "admin@traingo.vn", "ADMIN", "01/08/2026"),
			new User("Nguyễn Văn An", "an.nguyen@example.com", "CUSTOMER", "12/08/2026"),
			new User("Trần Thị Bình", "binh.tran@example.com", "CUSTOMER", "15/08/2026"),
			new User("Lê Hoàng Cường", "cuong.le@example.com", "CUSTOMER", "18/08/2026"),
			new User("Phạm Thu Dung", "dung.pham@example.com", "CUSTOMER", "20/08/2026"));

	private static final long TOTAL_REVENUE = 9_650_000L;

	public List<Station> stations() {
		return STATIONS;
	}

	public List<Route> routes() {
		return ROUTES;
	}

	public List<Train> trains() {
		return TRAINS;
	}

	public List<Trip> trips() {
		return TRIPS;
	}

	public List<Booking> bookings() {
		return BOOKINGS;
	}

	public List<User> users() {
		return USERS;
	}

	public long totalRevenue() {
		return TOTAL_REVENUE;
	}

}
