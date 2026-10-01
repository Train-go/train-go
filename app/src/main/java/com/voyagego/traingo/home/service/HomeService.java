package com.voyagego.traingo.home.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.voyagego.traingo.home.model.PopularRoute;

/**
 * Labels for the landing page's search form and popular route cards. Stations
 * and routes are in the database now, but the search form keeps these static
 * labels until trip search (stage 3 of docs/plans/active/mvp-traingo.md)
 * decides what the form submits, a station id or a code. Replace both lists
 * with repository lookups then.
 */
@Service
public class HomeService {

	private static final List<String> STATIONS = List.of(
		"Ga Sài Gòn",
		"Ga Nha Trang",
		"Ga Quy Nhơn",
		"Ga Đà Nẵng",
		"Ga Huế",
		"Ga Vinh",
		"Ga Hà Nội",
		"Ga Lào Cai");

	private static final List<PopularRoute> POPULAR_ROUTES = List.of(
		new PopularRoute("Ga Sài Gòn", "Ga Nha Trang", "Dọc duyên hải Nam Trung Bộ"),
		new PopularRoute("Ga Sài Gòn", "Ga Đà Nẵng", "Xuyên miền Trung"),
		new PopularRoute("Ga Hà Nội", "Ga Đà Nẵng", "Qua đèo Hải Vân"),
		new PopularRoute("Ga Hà Nội", "Ga Lào Cai", "Lên vùng cao Tây Bắc"));

	public List<String> stations() {
		return STATIONS;
	}

	public List<PopularRoute> popularRoutes() {
		return POPULAR_ROUTES;
	}

}
