package com.voyagego.traingo.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Page controllers answer a missing station, trip or booking with the
 * Vietnamese 404 page (templates/error/404.html) instead of a stack trace.
 * The exception's own sentence, such as "Không tìm thấy booking.", tells the
 * user what was missing. Business rule errors stay with each controller, which
 * shows them on the form the user was filling in; the one exception is a trip
 * that can no longer be booked, which has its own page.
 *
 * REST controllers are handled first by {@link ApiExceptionHandler}.
 */
@ControllerAdvice(annotations = Controller.class)
public class PageExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ModelAndView notFound(ResourceNotFoundException ex) {
		return new ModelAndView("error/404").addObject("notFoundMessage", ex.getMessage());
	}

	/** A trip that is missing, cancelled or gone is a page with a way back to the trip list, not a 404. */
	@ExceptionHandler(TripNotBookableException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	ModelAndView tripNotBookable(TripNotBookableException ex) {
		return new ModelAndView("error/trip-unavailable").addObject("reason", ex.getMessage());
	}

}
