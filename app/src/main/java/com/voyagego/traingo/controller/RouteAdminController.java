package com.voyagego.traingo.controller;

import java.util.List;
import java.util.stream.Collectors;

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

import com.voyagego.traingo.common.exception.DuplicateRouteException;
import com.voyagego.traingo.common.exception.RouteInUseException;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.model.request.RouteCreateRequest;
import com.voyagego.traingo.model.request.RouteDistanceRequest;
import com.voyagego.traingo.model.response.RouteRow;
import com.voyagego.traingo.service.RouteService;
import com.voyagego.traingo.service.StationService;

import jakarta.validation.Valid;

/**
 * Route management using Post/Redirect/Get. Invalid forms keep the submitted
 * values; successful changes return to the list with a flash message.
 */
@Controller
@RequestMapping("/admin/routes")
public class RouteAdminController {

	private static final String LIST_VIEW = "admin/routes";
	private static final String FORM_VIEW = "admin/route-form";
	private static final String REDIRECT_TO_LIST = "redirect:/admin/routes";

	private final RouteService routeService;
	private final StationService stationService;

	public RouteAdminController(RouteService routeService, StationService stationService) {
		this.routeService = routeService;
		this.stationService = stationService;
	}

	/** Every page of this controller highlights the same sidebar entry. */
	@ModelAttribute("activeNav")
	String activeNav() {
		return "routes";
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "") String q, Model model) {
		model.addAttribute("routes", routeService.search(q));
		model.addAttribute("q", q.strip());
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		RouteCreateRequest form = new RouteCreateRequest();
		form.setBothWays(true);
		model.addAttribute("routeForm", form);
		return showCreateForm(model);
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("routeForm") RouteCreateRequest form, BindingResult bindingResult,
			@RequestParam(defaultValue = "false") boolean distanceEdited, Model model, RedirectAttributes redirectAttributes) {
		model.addAttribute("distanceEdited", distanceEdited);
		// The cross-field constraint belongs beside the destination selector.
		var differentStations = bindingResult.getFieldError("differentStations");
		if (differentStations != null) {
			bindingResult.rejectValue("destinationStationId", "differentStations", differentStations.getDefaultMessage());
		}
		if (bindingResult.hasErrors()) {
			return showCreateForm(model);
		}
		List<Route> created;
		try {
			created = routeService.create(form);
		} catch (DuplicateRouteException ex) {
			bindingResult.rejectValue(ex.getField(), "duplicate", ex.getMessage());
			return showCreateForm(model);
		}
		String names = created.stream().map(route -> route.getOriginStation().getName() + " → "
				+ route.getDestinationStation().getName()).collect(Collectors.joining(", "));
		redirectAttributes.addFlashAttribute("successMessage", "Đã thêm " + created.size() + " tuyến: " + names + ".");
		return REDIRECT_TO_LIST;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable long id, Model model) {
		RouteRow route = routeService.findRow(id);
		RouteDistanceRequest form = new RouteDistanceRequest();
		form.setDistanceKm(route.getDistanceKm());
		model.addAttribute("routeForm", form);
		return showEditForm(model, route);
	}

	@PostMapping("/{id}")
	public String update(@PathVariable long id, @Valid @ModelAttribute("routeForm") RouteDistanceRequest form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		RouteRow route = routeService.findRow(id);
		if (bindingResult.hasErrors()) {
			return showEditForm(model, route);
		}
		routeService.changeDistance(id, form.getDistanceKm());
		// Use the detached row: changeDistance returns a route with lazy stations.
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tuyến " + route.getOriginName()
				+ " → " + route.getDestinationName() + ".");
		return REDIRECT_TO_LIST;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
		try {
			routeService.delete(id);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tuyến.");
		} catch (RouteInUseException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return REDIRECT_TO_LIST;
	}

	private String showCreateForm(Model model) {
		model.addAttribute("stations", stationService.findAll());
		return FORM_VIEW;
	}

	private static String showEditForm(Model model, RouteRow route) {
		model.addAttribute("routeId", route.getId());
		model.addAttribute("route", route);
		return FORM_VIEW;
	}

}
