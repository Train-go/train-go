package com.voyagego.traingo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.request.TripSearchRequest;
import com.voyagego.traingo.model.response.DateTab;
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.model.response.TripCard;
import com.voyagego.traingo.model.response.TripSearchResult;
import com.voyagego.traingo.service.HomeService;
import com.voyagego.traingo.service.TripSearchService;

import jakarta.validation.Valid;

/**
 * Step 1 of a booking, the trip list (issue #26). The page owns its search:
 * changing stations or day stays here and never sends the customer back to
 * the homepage. Guests may use it; picking a seat type is what needs a login.
 */
@Controller
public class TripSearchController {

	private static final String VIEW = "trips/search";
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final TripSearchService tripSearchService;
	private final HomeService homeService;

	public TripSearchController(TripSearchService tripSearchService, HomeService homeService) {
		this.tripSearchService = tripSearchService;
		this.homeService = homeService;
	}

	@ModelAttribute("activeNav")
	String activeNav() {
		return "search";
	}

	@GetMapping("/trips")
	public String search(@Valid @ModelAttribute("search") TripSearchRequest search, BindingResult bindingResult,
			Model model) {
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = now.toLocalDate();

		// A missing, unreadable or past day becomes today in the address bar too,
		// so the link the customer copies is the list they see.
		if (search.getDate() == null || search.getDate().isBefore(today)) {
			search.setDate(today);
			if (search.hasRoute()) {
				return "redirect:" + search.url(today);
			}
		}

		model.addAttribute("stations", homeService.stations());
		if (!search.hasRoute()) {
			return VIEW;
		}

		Optional<Station> origin = tripSearchService.findStation(search.getFrom());
		Optional<Station> destination = tripSearchService.findStation(search.getTo());
		if (origin.isEmpty()) {
			bindingResult.rejectValue("from", "unknown", "Không tìm thấy ga đi này.");
		}
		if (destination.isEmpty()) {
			bindingResult.rejectValue("to", "unknown", "Không tìm thấy ga đến này.");
		}
		if (bindingResult.hasFieldErrors("from") || bindingResult.hasFieldErrors("to")
				|| bindingResult.hasFieldErrors("differentStations")) {
			return VIEW;
		}

		LocalDate day = search.getDate();
		TripSearchResult result = tripSearchService.search(search, origin.get(), destination.get(), now);
		List<DateTab> strip = tripSearchService.dateStrip(day, today);
		boolean stripStartsToday = strip.get(0).date().equals(today);
		LocalDate weekBack = day.minusDays(TripSearchService.STRIP_DAYS);

		model.addAttribute("originName", PopularRoute.placeName(origin.get().getName()));
		model.addAttribute("destinationName", PopularRoute.placeName(destination.get().getName()));
		model.addAttribute("result", result);
		model.addAttribute("openTripId", result.cards().stream().filter(card -> !card.soldOut())
				.map(TripCard::tripId).findFirst().orElse(null));
		model.addAttribute("dateStrip", strip);
		model.addAttribute("dayTitle", TripSearchService.weekday(day) + ", " + day.format(DAY));
		model.addAttribute("today", today);
		model.addAttribute("prevWeekUrl", stripStartsToday ? null
				: search.url(weekBack.isBefore(today) ? today : weekBack));
		model.addAttribute("nextWeekUrl", search.url(day.plusDays(TripSearchService.STRIP_DAYS)));
		model.addAttribute("stepsTripsUrl", search.url(day));
		return VIEW;
	}

}
