package com.voyagego.traingo.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.model.request.ProfileRequest;
import com.voyagego.traingo.service.TrainGoUserDetails;
import com.voyagego.traingo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * "Hồ sơ", /profile: a signed-in user reads their details and changes their name
 * and phone number. The email is the login, so it is shown but not editable.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

	private static final String VIEW = "profile/index";

	private final UserService userService;
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	public ProfileController(UserService userService) {
		this.userService = userService;
	}

	@ModelAttribute("activeNav")
	String activeNav() {
		return "profile";
	}

	@GetMapping
	public String form(@AuthenticationPrincipal TrainGoUserDetails user, Model model) {
		var form = new ProfileRequest();
		form.setFullName(user.getFullName());
		form.setPhoneNumber(user.getPhoneNumber());
		model.addAttribute("profileForm", form);
		return VIEW;
	}

	@PostMapping
	public String update(@AuthenticationPrincipal TrainGoUserDetails user,
			@Valid @ModelAttribute("profileForm") ProfileRequest form, BindingResult bindingResult,
			HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return VIEW;
		}
		refreshPrincipal(userService.updateProfile(user.getUserId(), form), request, response);
		redirectAttributes.addFlashAttribute("successMessage", "Đã lưu hồ sơ.");
		return "redirect:/profile";
	}

	/** The session keeps a copy of the user, which the header and the booking pages read. */
	private void refreshPrincipal(User updated, HttpServletRequest request, HttpServletResponse response) {
		var principal = new TrainGoUserDetails(updated);
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
