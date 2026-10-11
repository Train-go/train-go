package com.voyagego.traingo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.voyagego.traingo.model.response.AdminBooking;
import com.voyagego.traingo.model.response.AdminRoute;
import com.voyagego.traingo.model.response.AdminTrain;
import com.voyagego.traingo.model.response.AdminTrip;
import com.voyagego.traingo.model.response.AdminUser;

/**
 * Placeholder data for the admin screens that do not read the database yet.
 * Each list goes away when its screen moves into its own controller and reads
 * the real repository, as stations already have. Coaches have no screen of
 * their own on purpose: they are managed inside a train's detail page.
 */
@Service
public class AdminService {

	private static final List<AdminRoute> ROUTES = List.of(new AdminRoute("SGN-NTR", "Ga Sài Gòn", "Ga Nha Trang", 411),
			new AdminRoute("SGN-DNG", "Ga Sài Gòn", "Ga Đà Nẵng", 935), new AdminRoute("HNI-DNG", "Ga Hà Nội", "Ga Đà Nẵng", 791),
			new AdminRoute("HNI-LCI", "Ga Hà Nội", "Ga Lào Cai", 296),
			new AdminRoute("HNI-SGN", "Ga Hà Nội", "Ga Sài Gòn", 1726));

	private static final List<AdminTrain> TRAINS = List.of(new AdminTrain("SE1", "Thống Nhất", 12, 560),
			new AdminTrain("SE2", "Thống Nhất", 12, 560), new AdminTrain("SE7", "Thống Nhất", 10, 448),
			new AdminTrain("SNT1", "Sài Gòn - Nha Trang", 8, 336), new AdminTrain("SP3", "Hà Nội - Lào Cai", 9, 372));

	private static final List<AdminTrip> TRIPS = List.of(
			new AdminTrip("SE1", "Ga Hà Nội — Ga Sài Gòn", "01/09/2026", "19:30", "05:10", 1_150_000, "Mở bán"),
			new AdminTrip("SE2", "Ga Sài Gòn — Ga Hà Nội", "01/09/2026", "20:00", "05:45", 1_150_000, "Mở bán"),
			new AdminTrip("SNT1", "Ga Sài Gòn — Ga Nha Trang", "02/09/2026", "21:15", "05:00", 420_000, "Mở bán"),
			new AdminTrip("SP3", "Ga Hà Nội — Ga Lào Cai", "02/09/2026", "22:00", "06:05", 380_000, "Đã khởi hành"),
			new AdminTrip("SE7", "Ga Hà Nội — Ga Đà Nẵng", "03/09/2026", "06:00", "21:40", 780_000, "Mở bán"));

	private static final List<AdminBooking> BOOKINGS = List.of(
			new AdminBooking("TG26080001", "Nguyễn Văn An", "SE1 · 01/09/2026", 2, 2_300_000, "Đã thanh toán"),
			new AdminBooking("TG26080002", "Trần Thị Bình", "SNT1 · 02/09/2026", 1, 420_000, "Chờ thanh toán"),
			new AdminBooking("TG26080003", "Lê Hoàng Cường", "SE2 · 01/09/2026", 4, 4_600_000, "Đã thanh toán"),
			new AdminBooking("TG26080004", "Phạm Thu Dung", "SE7 · 03/09/2026", 2, 1_560_000, "Đã hủy"),
			new AdminBooking("TG26080005", "Vũ Minh Đức", "SP3 · 02/09/2026", 3, 1_140_000, "Đã thanh toán"));

	// Read-only screen: admin.md gives this list no create, edit or delete.
	private static final List<AdminUser> USERS = List.of(
			new AdminUser("Quản trị viên", "admin@traingo.vn", "ADMIN", "01/08/2026"),
			new AdminUser("Nguyễn Văn An", "an.nguyen@example.com", "CUSTOMER", "12/08/2026"),
			new AdminUser("Trần Thị Bình", "binh.tran@example.com", "CUSTOMER", "15/08/2026"),
			new AdminUser("Lê Hoàng Cường", "cuong.le@example.com", "CUSTOMER", "18/08/2026"),
			new AdminUser("Phạm Thu Dung", "dung.pham@example.com", "CUSTOMER", "20/08/2026"));

	private static final long TOTAL_REVENUE = 9_650_000L;

	public List<AdminRoute> routes() {
		return ROUTES;
	}

	public List<AdminTrain> trains() {
		return TRAINS;
	}

	public List<AdminTrip> trips() {
		return TRIPS;
	}

	public List<AdminBooking> bookings() {
		return BOOKINGS;
	}

	public List<AdminUser> users() {
		return USERS;
	}

	public long totalRevenue() {
		return TOTAL_REVENUE;
	}

}
