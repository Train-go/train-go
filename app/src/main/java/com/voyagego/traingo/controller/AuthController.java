package com.voyagego.traingo.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.voyagego.traingo.common.config.EmailRetainingLoginFailureHandler;
import com.voyagego.traingo.common.exception.DuplicateEmailException;
import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.model.request.RegisterRequest;
import com.voyagego.traingo.service.TrainGoUserDetails;
import com.voyagego.traingo.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * The login and register pages (docs/product/auth.md). POST /login is not here:
 * Spring Security's filter handles it and EmailRetainingLoginFailureHandler /
 * RoleBasedLoginSuccessHandler decide where it goes next.
 *
 * Someone who is already signed in is sent to their own home instead of seeing
 * either form, and cannot create another account from that session.
 */
@Controller
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private static final String LOGIN_VIEW = "auth/login";
	private static final String REGISTER_VIEW = "auth/register";

	private final UserService userService;
	private final RequestCache requestCache;

	public AuthController(UserService userService, RequestCache requestCache) {
		this.userService = userService;
		this.requestCache = requestCache;
	}

	@GetMapping("/login")
	public String loginForm(@AuthenticationPrincipal TrainGoUserDetails user, HttpServletRequest request,
			@RequestParam(name = "error", required = false) String error, Model model) {
		if (user != null) {
			return homeOf(user);
		}
		model.addAttribute("loginFailed", error != null);
		HttpSession session = request.getSession(false);
		if (session != null) {
			model.addAttribute("loginEmail", session.getAttribute(EmailRetainingLoginFailureHandler.LOGIN_EMAIL_ATTRIBUTE));
			session.removeAttribute(EmailRetainingLoginFailureHandler.LOGIN_EMAIL_ATTRIBUTE);
		}
		return LOGIN_VIEW;
	}

	@GetMapping("/register")
	public String registerForm(@AuthenticationPrincipal TrainGoUserDetails user, Model model) {
		if (user != null) {
			return homeOf(user);
		}
		model.addAttribute("registerForm", new RegisterRequest());
		model.addAttribute("fieldErrors", Map.of());
		return REGISTER_VIEW;
	}

	@PostMapping("/register")
	public String register(@AuthenticationPrincipal TrainGoUserDetails user,
			@Valid @ModelAttribute("registerForm") RegisterRequest form, BindingResult bindingResult,
			Model model, HttpServletRequest request, HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		if (user != null) {
			return homeOf(user);
		}
		Map<String, String> fieldErrors = fieldErrors(bindingResult);
		User created = null;
		if (fieldErrors.isEmpty()) {
			try {
				created = userService.register(form);
			} catch (DuplicateEmailException ex) {
				fieldErrors.put(ex.getField(), ex.getMessage());
			}
		}
		if (created == null) {
			form.setPassword(null);
			form.setConfirmPassword(null);
			model.addAttribute("fieldErrors", fieldErrors);
			return REGISTER_VIEW;
		}
		return signIn(created, form.getPassword(), request, response, redirectAttributes);
	}

	/**
	 * Signs the new customer in once the registration has committed. A failure
	 * here does not undo the account: the customer is sent to the login page
	 * with a notice instead of being asked to register again.
	 */
	private String signIn(User created, String rawPassword, HttpServletRequest request,
			HttpServletResponse response, RedirectAttributes redirectAttributes) {
		// A page the visitor wanted before registering must not decide a later login.
		requestCache.removeRequest(request, response);
		if (request.getSession(false) != null) {
			// Same protection against session fixation that the login filter applies.
			request.changeSessionId();
		}
		try {
			request.login(created.getEmail(), rawPassword);
		} catch (ServletException ex) {
			log.warn("Sign-in after registering user {} failed: {}", created.getId(), ex.getClass().getSimpleName());
			redirectAttributes.addFlashAttribute("accountCreatedMessage", "Tài khoản đã được tạo. Vui lòng đăng nhập.");
			return "redirect:/login";
		}
		return "redirect:/";
	}

	/**
	 * First message per form field. The two bean-level checks of RegisterRequest
	 * are reported on the field the visitor has to fix, and only after the
	 * field's own messages so "retype the password" wins over "do not match".
	 */
	private static Map<String, String> fieldErrors(BindingResult bindingResult) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : bindingResult.getFieldErrors()) {
			if (!isBeanLevelCheck(error.getField())) {
				errors.putIfAbsent(error.getField(), error.getDefaultMessage());
			}
		}
		for (FieldError error : bindingResult.getFieldErrors()) {
			if (isBeanLevelCheck(error.getField())) {
				errors.putIfAbsent(fieldOf(error.getField()), error.getDefaultMessage());
			}
		}
		return errors;
	}

	private static boolean isBeanLevelCheck(String field) {
		return field.equals("passwordConfirmed") || field.equals("passwordWithinLimit");
	}

	private static String fieldOf(String beanLevelCheck) {
		return beanLevelCheck.equals("passwordConfirmed") ? "confirmPassword" : "password";
	}

	private static String homeOf(TrainGoUserDetails user) {
		return user.getRole() == UserRole.ADMIN ? "redirect:/admin" : "redirect:/";
	}

}
