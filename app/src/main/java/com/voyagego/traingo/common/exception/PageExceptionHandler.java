package com.voyagego.traingo.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Page controllers answer a missing station, trip or booking with the
 * Vietnamese 404 page (templates/error/404.html) instead of a stack trace.
 * Business rule errors stay with each controller, which shows them on the form
 * the user was filling in.
 *
 * REST controllers are handled first by {@link ApiExceptionHandler}.
 */
@ControllerAdvice(annotations = Controller.class)
public class PageExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	String notFound() {
		return "error/404";
	}

}
