package com.voyagego.traingo.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import com.voyagego.traingo.common.exception.SeatSelectionException;
import com.voyagego.traingo.common.exception.SeatTakenException;
import com.voyagego.traingo.common.exception.TripNotBookableException;
import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.CoachType;
import com.voyagego.traingo.model.dto.BookingDraft;
import com.voyagego.traingo.model.request.PassengersForm;
import com.voyagego.traingo.model.response.SeatMap.TripSummary;
import com.voyagego.traingo.service.BookingDraftService;
import com.voyagego.traingo.service.BookingDraftStore;
import com.voyagego.traingo.service.MyTripsService;
import com.voyagego.traingo.service.PaymentFacade;
import com.voyagego.traingo.service.PaymentService;
import com.voyagego.traingo.service.SeatMapService;
import com.voyagego.traingo.service.TrainGoUserDetails;
import com.voyagego.traingo.service.TripSearchService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * The booking steps after the trip list. SecurityConfig lets only customers
 * into /booking/**; a guest who picks a seat type on the trip list signs in
 * first and returns to this page.
 *
 * Step 2, the seat page, shows the map from the JSON API (seat-map.js) and
 * keeps the chosen seats in the session draft. Step 3, the passenger page,
 * fills the draft's names and CCCD, one ticket per seat. Neither writes to
 * the database. The payment step adds its handlers here.
 */
@Controller
@RequestMapping("/booking")
public class BookingFlowController {

	private static final String SEATS_VIEW = "booking/seats";
	private static final String PASSENGERS_VIEW = "booking/passengers";
	private static final String PAYMENT_VIEW = "booking/payment";
	private static final String SUCCESS_VIEW = "booking/success";
	private static final String NO_DRAFT = "Hãy chọn chuyến và chỗ trước.";
	/** Session attribute: the booking this session paid last, so a repeated payment finds it. */
	private static final String LAST_BOOKING = "lastBookingId";
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final SeatMapService seatMapService;
	private final BookingDraftService bookingDraftService;
	private final BookingDraftStore bookingDraftStore;
	private final PaymentService paymentService;
	private final PaymentFacade paymentFacade;
	private final MyTripsService myTripsService;

	public BookingFlowController(SeatMapService seatMapService, BookingDraftService bookingDraftService,
			BookingDraftStore bookingDraftStore, PaymentService paymentService, PaymentFacade paymentFacade,
			MyTripsService myTripsService) {
		this.seatMapService = seatMapService;
		this.bookingDraftService = bookingDraftService;
		this.bookingDraftStore = bookingDraftStore;
		this.paymentService = paymentService;
		this.paymentFacade = paymentFacade;
		this.myTripsService = myTripsService;
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

	/** With nothing chosen yet there is nothing to fill in: back to the trip list. */
	@GetMapping("/passengers")
	public String passengers(@AuthenticationPrincipal TrainGoUserDetails user, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		BookingDraft draft = bookingDraftStore.get(session).orElse(null);
		if (draft == null) {
			redirectAttributes.addFlashAttribute("errorMessage", NO_DRAFT);
			return "redirect:/trips";
		}
		model.addAttribute("form", bookingDraftService.formOf(draft));
		return passengerPage(draft, user, model);
	}

	@PostMapping("/passengers")
	public String savePassengers(@Valid @ModelAttribute("form") PassengersForm form, BindingResult errors,
			@AuthenticationPrincipal TrainGoUserDetails user, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		BookingDraft draft = bookingDraftStore.get(session).orElse(null);
		if (draft == null) {
			redirectAttributes.addFlashAttribute("errorMessage", NO_DRAFT);
			return "redirect:/trips";
		}
		if (errors.hasErrors()) {
			return passengerPage(draft, user, model);
		}
		try {
			bookingDraftStore.save(session, bookingDraftService.updatePassengers(draft, form));
		} catch (SeatSelectionException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/booking/passengers";
		}
		return "redirect:/booking/payment";
	}

	/**
	 * "Chọn lại chỗ" on the passenger page: keeps what was typed so far, then
	 * opens the seat page of the same trip with the chosen seats ticked.
	 */
	@PostMapping("/passengers/reselect")
	public String reselect(@ModelAttribute("form") PassengersForm form, HttpSession session,
			RedirectAttributes redirectAttributes) {
		BookingDraft draft = bookingDraftStore.get(session).orElse(null);
		if (draft == null) {
			redirectAttributes.addFlashAttribute("errorMessage", NO_DRAFT);
			return "redirect:/trips";
		}
		bookingDraftStore.save(session, bookingDraftService.keepEdits(draft, form));
		return "redirect:/booking/" + draft.getTripId() + "/seats";
	}

	/** Step 4: the bill. A draft that is not ready to pay goes back to the passenger step. */
	@GetMapping("/payment")
	public String payment(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		BookingDraft draft = bookingDraftStore.get(session).orElse(null);
		if (draft == null) {
			redirectAttributes.addFlashAttribute("errorMessage", NO_DRAFT);
			return "redirect:/trips";
		}
		try {
			var invoice = paymentService.invoice(draft, LocalDateTime.now());
			model.addAttribute("invoice", invoice);
			addTripLinks(model, invoice.trip());
			return PAYMENT_VIEW;
		} catch (SeatSelectionException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/booking/passengers";
		}
	}

	/**
	 * Pays the draft. The session is locked for the whole call, so a second
	 * press of the button, or a second tab, waits for the first and then finds
	 * no draft: it is sent to the booking the first one made instead of
	 * answering "seat taken" for seats that are now the customer's own.
	 */
	@PostMapping("/payment")
	public String pay(@AuthenticationPrincipal TrainGoUserDetails user, HttpSession session,
			RedirectAttributes redirectAttributes) {
		synchronized (WebUtils.getSessionMutex(session)) {
			BookingDraft draft = bookingDraftStore.get(session).orElse(null);
			if (draft == null) {
				if (session.getAttribute(LAST_BOOKING) instanceof Long lastId) {
					return "redirect:/booking/success/" + lastId;
				}
				redirectAttributes.addFlashAttribute("errorMessage", NO_DRAFT);
				return "redirect:/trips";
			}
			try {
				Booking booking = paymentFacade.pay(draft, user.getUserId(), LocalDateTime.now());
				bookingDraftStore.clear(session);
				session.setAttribute(LAST_BOOKING, booking.getId());
				return "redirect:/booking/success/" + booking.getId();
			} catch (SeatTakenException ex) {
				bookingDraftService.withoutSeats(draft, ex.getSeatIds()).ifPresentOrElse(
						rest -> bookingDraftStore.save(session, rest), () -> bookingDraftStore.clear(session));
				redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
				return "redirect:/booking/" + draft.getTripId() + "/seats";
			} catch (TripNotBookableException ex) {
				bookingDraftStore.clear(session);
				redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
				return "redirect:/trips";
			} catch (SeatSelectionException ex) {
				redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
				return "redirect:/booking/passengers";
			}
		}
	}

	/** The paid booking. Someone else's booking is the 404 page, as in "Vé của tôi". */
	@GetMapping("/success/{bookingId}")
	public String success(@PathVariable long bookingId, @AuthenticationPrincipal TrainGoUserDetails user,
			Model model) {
		model.addAttribute("booking", myTripsService.detail(user.getUserId(), bookingId, LocalDateTime.now()));
		return SUCCESS_VIEW;
	}

	private String passengerPage(BookingDraft draft, TrainGoUserDetails user, Model model) {
		var page = bookingDraftService.passengerPage(draft, user, LocalDateTime.now());
		model.addAttribute("page", page);
		addTripLinks(model, page.trip());
		return PASSENGERS_VIEW;
	}

	private String seatPage(long tripId, String type, List<Long> chosen, Model model) {
		TripSummary trip = seatMapService.seatMap(tripId, LocalDateTime.now()).trip();
		model.addAttribute("trip", trip);
		model.addAttribute("coachType", coachType(type));
		model.addAttribute("selectedSeatIds", chosen);
		addTripLinks(model, trip);
		return SEATS_VIEW;
	}

	/** What every booking page needs: the day line, and the links of the steps already passed. */
	private static void addTripLinks(Model model, TripSummary trip) {
		model.addAttribute("dayLabel", TripSearchService.weekday(trip.departureAt().toLocalDate()) + ", "
				+ trip.departureAt().format(DAY));
		// Step 1 returns to the search that found the trip, step 2 to this trip's seat page.
		model.addAttribute("stepsTripsUrl", UriComponentsBuilder.fromPath("/trips")
				.queryParam("from", trip.originCode())
				.queryParam("to", trip.destinationCode())
				.queryParam("date", trip.departureAt().toLocalDate())
				.encode().toUriString());
		model.addAttribute("stepsSeatsUrl", "/booking/" + trip.tripId() + "/seats");
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
