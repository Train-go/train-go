package com.voyagego.traingo.home;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	/**
	 * Placeholder data until the Station entity exists (stage 2 of
	 * docs/plans/active/mvp-traingo.md). The landing page only needs labels to
	 * fill the search form and the popular route cards, so nothing here reaches
	 * the database. Replace both lists with StationRepository lookups then.
	 */
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

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("appName", "TrainGo");
		model.addAttribute("stations", STATIONS);
		model.addAttribute("popularRoutes", POPULAR_ROUTES);
		return "index";
	}

	/**
	 * A route shown on the landing page. Carries no price or duration: those
	 * come from a real Trip, and inventing them here would put numbers on screen
	 * that no one can trace back to data.
	 */
	public record PopularRoute(String origin, String destination, String note) {
	}

}
