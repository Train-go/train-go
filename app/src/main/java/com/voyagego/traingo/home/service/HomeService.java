package com.voyagego.traingo.home.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.voyagego.traingo.home.model.PopularRoute;

/**
 * Placeholder data until the Station entity exists (stage 2 of
 * docs/plans/active/mvp-traingo.md). The landing page only needs labels to
 * fill the search form and the popular route cards, so nothing here reaches
 * the database. Replace both lists with StationRepository lookups then.
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
