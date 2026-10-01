package com.voyagego.traingo.admin.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Model data the admin header needs on every admin page, whichever feature
 * package the page's controller lives in.
 *
 * It applies to every controller, so customer pages also receive an attribute
 * they never read. Scoping it would mean a list of admin controllers that each
 * new screen has to remember to join, and forgetting only shows up as a blank
 * name in the header. Delete this class when the auth feature lands and the
 * header shows the signed-in user instead.
 */
@ControllerAdvice
public class AdminShellModelAdvice {

	@ModelAttribute("adminName")
	String adminName() {
		return "Quản trị viên";
	}

}
