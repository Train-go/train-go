package com.voyagego.traingo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.voyagego.traingo.service.TrainGoUserDetails;

@ControllerAdvice
public class CurrentUserModelAdvice {
	@ModelAttribute("currentUser")
	public TrainGoUserDetails currentUser(@AuthenticationPrincipal TrainGoUserDetails user) {
		return user;
	}
}
