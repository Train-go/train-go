package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.voyagego.traingo.common.exception.SeatSelectionException;
import com.voyagego.traingo.common.exception.SeatTakenException;
import com.voyagego.traingo.entity.CoachType;
import com.voyagego.traingo.model.dto.BookingDraft;
import com.voyagego.traingo.model.response.SeatMap.TripSummary;
import com.voyagego.traingo.service.BookingDraftService;
import com.voyagego.traingo.service.BookingDraftStore;
import com.voyagego.traingo.service.SeatMapService;
import com.voyagego.traingo.service.TrainGoUserDetails;
import com.voyagego.traingo.service.TripSearchService;

import jakarta.servlet.http.HttpSession;

/**
 * The booking steps after the trip list. SecurityConfig lets only customers
 * into /booking/**; a guest who picks a seat type on the trip list signs in
 * first and returns to this page.
 *
 * Step 2, the seat page, shows the map from the JSON API (seat-map.js) and
 * keeps the chosen seats in the session draft. The passenger and payment
 * steps add their handlers here.
 */
@Controller
@RequestMapping("/booking")
public class BookingFlowController {

	private static final String SEATS_VIEW = "booking/seats";
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final SeatMapService seatMapService;
	private final BookingDraftService bookingDraftService;
	private final BookingDraftStore bookingDraftStore;

	public BookingFlowController(SeatMapService seatMapService, BookingDraftService bookingDraftService,
			BookingDraftStore bookingDraftStore) {
		this.seatMapService = seatMapService;
		this.bookingDraftService = bookingDraftService;
		this.bookingDraftStore = bookingDraftStore;
	}

	/** Every booking page highlights the search item of the navbar. */
	@ModelAttribute("activeNav")
	String activeNav() {
		return "search";
	}

	@GetMapping("/{tripId}/seats")
	public String seats(@PathVariable long tripId, @RequestParam(required = false) String type,
			HttpSession session, Model model) {
		List<Long> chosen = bookingDraftStore.get(session)
				.filter(draft -> draft.getTripId() == tripId)
				.map(BookingDraft::getTripSeatIds)
				.orElse(List.of());
		return seatPage(tripId, type, chosen, model);
	}

	@PostMapping("/{tripId}/seats")
	public String chooseSeats(@PathVariable long tripId, @RequestParam(required = false) String type,
			@RequestParam(name = "seatIds", required = false) List<Long> seatIds,
			@AuthenticationPrincipal TrainGoUserDetails user, HttpSession session, Model model) {
		List<Long> requested = seatIds == null ? List.of() : seatIds;
		try {
			BookingDraft previous = bookingDraftStore.get(session).orElse(null);
			bookingDraftStore.save(session,
					bookingDraftService.chooseSeats(tripId, requested, user, previous, LocalDateTime.now()));
			return "redirect:/booking/passengers";
		} catch (SeatTakenException ex) {
			// The other seats stay chosen; the page reloads the map, so the taken seat shows as sold.
			model.addAttribute("errorMessage", ex.getMessage());
			return seatPage(tripId, type, requested.stream().filter(id -> !ex.getSeatIds().contains(id)).toList(),
					model);
		} catch (SeatSelectionException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			return seatPage(tripId, type, requested, model);
		}
	}

	private String seatPage(long tripId, String type, List<Long> chosen, Model model) {
		TripSummary trip = seatMapService.seatMap(tripId, LocalDateTime.now()).trip();
		String tripsUrl = UriComponentsBuilder.fromPath("/trips")
				.queryParam("from", trip.originCode())
				.queryParam("to", trip.destinationCode())
				.queryParam("date", trip.departureAt().toLocalDate())
				.encode().toUriString();
		model.addAttribute("trip", trip);
		model.addAttribute("dayLabel", TripSearchService.weekday(trip.departureAt().toLocalDate()) + ", "
				+ trip.departureAt().format(DAY));
		model.addAttribute("coachType", coachType(type));
		model.addAttribute("selectedSeatIds", chosen);
		model.addAttribute("stepsTripsUrl", tripsUrl);
		return SEATS_VIEW;
	}

	/** SEAT or SLEEPER, else null: an unknown value only means no coach type was asked for. */
	private static CoachType coachType(String type) {
		for (CoachType value : CoachType.values()) {
			if (value.name().equalsIgnoreCase(type)) {
				return value;
			}
		}
		return null;
	}

}
