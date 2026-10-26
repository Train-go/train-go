package com.voyagego.traingo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.service.UserAdminService;

/** The read-only user list (docs/product/admin.md, "Danh sách người dùng"). */
@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

	private final UserAdminService userAdminService;

	public UserAdminController(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	@ModelAttribute("activeNav")
	String activeNav() {
		return "users";
	}

	/** {@code role} is ADMIN or CUSTOMER; anything else opens the customer tab. */
	@GetMapping
	public String list(@RequestParam(required = false) String role, Model model) {
		model.addAttribute("view", userAdminService.list("ADMIN".equals(role) ? UserRole.ADMIN : UserRole.CUSTOMER));
		return "admin/users";
	}

}
