package com.voyagego.traingo.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.voyagego.traingo.common.exception.DuplicateStationCodeException;
import com.voyagego.traingo.common.exception.StationInUseException;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.request.StationRequest;
import com.voyagego.traingo.service.StationService;

import jakarta.validation.Valid;

/**
 * Station management, /admin/stations in docs/product/admin.md. It is its own
 * controller rather than part of AdminController so each admin screen calls its
 * own service (docs/decisions/0006-cau-truc-package-theo-tang.md).
 *
 * Every successful POST redirects back to the list (Post/Redirect/Get), so
 * reloading the list never submits the form a second time. A form with errors
 * is rendered again with what the user typed.
 */
@Controller
@RequestMapping("/admin/stations")
public class StationAdminController {

	private static final String LIST_VIEW = "admin/stations";
	private static final String FORM_VIEW = "admin/station-form";
	private static final String REDIRECT_TO_LIST = "redirect:/admin/stations";

	private final StationService stationService;

	public StationAdminController(StationService stationService) {
		this.stationService = stationService;
	}

	/**
	 * Trims every submitted text field and turns blank ones into null, so
	 * "  SGN " is validated as "SGN" and an empty city is stored as no city.
	 */
	@InitBinder
	void trimSubmittedText(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
	}

	/** Every page of this controller highlights the same sidebar entry. */
	@ModelAttribute("activeNav")
	String activeNav() {
		return "stations";
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("stations", stationService.findAll());
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("stationForm", new StationRequest());
		return FORM_VIEW;
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("stationForm") StationRequest form, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return FORM_VIEW;
		}
		Station station;
		try {
			station = stationService.create(form);
		} catch (DuplicateStationCodeException | DataIntegrityViolationException ex) {
			rejectDuplicateCode(bindingResult);
			return FORM_VIEW;
		}
		redirectAttributes.addFlashAttribute("successMessage", "Đã thêm " + describe(station) + ".");
		return REDIRECT_TO_LIST;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable long id, Model model) {
		model.addAttribute("stationId", id);
		model.addAttribute("stationForm", StationRequest.from(stationService.findById(id)));
		return FORM_VIEW;
	}

	@PostMapping("/{id}")
	public String update(@PathVariable long id, @Valid @ModelAttribute("stationForm") StationRequest form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		// The form posts back to this station's URL, also when it is shown again.
		model.addAttribute("stationId", id);
		if (bindingResult.hasErrors()) {
			return FORM_VIEW;
		}
		Station station;
		try {
			station = stationService.update(id, form);
		} catch (DuplicateStationCodeException | DataIntegrityViolationException ex) {
			rejectDuplicateCode(bindingResult);
			return FORM_VIEW;
		}
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật " + describe(station) + ".");
		return REDIRECT_TO_LIST;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
		try {
			Station station = stationService.delete(id);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa " + describe(station) + ".");
		} catch (StationInUseException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không xóa được " + describe(ex.getStation())
					+ " vì ga đang là điểm đi hoặc điểm đến của một tuyến.");
		}
		return REDIRECT_TO_LIST;
	}

	private static void rejectDuplicateCode(BindingResult bindingResult) {
		bindingResult.rejectValue("code", "duplicate", "Mã ga này đã được dùng cho một ga khác.");
	}

	private static String describe(Station station) {
		return station.getName() + " (" + station.getCode() + ")";
	}

}
