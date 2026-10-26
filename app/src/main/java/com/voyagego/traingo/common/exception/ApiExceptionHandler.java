package com.voyagego.traingo.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.voyagego.traingo.model.request.RouteCreateRequest;
import com.voyagego.traingo.model.response.ApiError;

/**
 * Turns the project's exceptions into an {@link ApiError} for every
 * {@code @RestController}: 400 for a form that fails validation, 404 for
 * {@link ResourceNotFoundException}, 409 for {@link BusinessRuleException}.
 * Anything else still answers in the same shape, so a script never has to
 * parse Spring Boot's own error JSON, which can carry a stack trace.
 *
 * Ordered first on purpose. {@code @RestController} carries {@code @Controller},
 * so {@link PageExceptionHandler} applies to REST controllers too; without the
 * order Spring may pick the page handler and answer an API call with HTML.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	static final String INVALID_FORM = "Vui lòng kiểm tra lại các ô được đánh dấu.";
	static final String UNREADABLE_BODY = "Dữ liệu gửi lên không đúng định dạng.";
	static final String BAD_REQUEST = "Yêu cầu không hợp lệ.";
	static final String SERVER_ERROR = "Hệ thống đang gặp sự cố. Vui lòng thử lại sau ít phút.";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalidForm(MethodArgumentNotValidException ex) {
		// First message per field: two failed rules on one input show one line.
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			String field = error.getField();
			if (ex.getBindingResult().getTarget() instanceof RouteCreateRequest
					&& "differentStations".equals(field)) {
				field = "destinationStationId";
			}
			fields.putIfAbsent(field, error.getDefaultMessage());
		}
		return ResponseEntity.badRequest().body(new ApiError(INVALID_FORM, fields));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> unreadableBody() {
		return ResponseEntity.badRequest().body(ApiError.of(UNREADABLE_BODY));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(ex.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	ResponseEntity<ApiError> ruleBroken(BusinessRuleException ex) {
		Map<String, String> fields = ex.getField() == null ? Map.of() : Map.of(ex.getField(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage(), fields));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> conflictingWrite(DataIntegrityViolationException ex) {
		// Concurrent requests can pass the service checks before a unique key
		// rejects one write. Handle it after rollback, without exposing SQL.
		String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
		if (cause.contains("uk_stations_code")) {
			return ruleBroken(new DuplicateStationCodeException());
		}
		if (cause.contains("uk_stations_km_mark")) {
			String message = "Lý trình đã được một ga khác sử dụng. Vui lòng tải lại và thử lại.";
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new ApiError(message, Map.of("kmMark", message)));
		}
		return unexpected(ex);
	}

	/**
	 * Spring's own request errors (a missing parameter, a wrong type) keep
	 * their 4xx status. Everything else is a bug: it is logged here, because a
	 * handled exception is no longer logged by Spring, and the caller gets a
	 * 500 without any detail.
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		if (ex instanceof ErrorResponse response && response.getStatusCode().is4xxClientError()) {
			return ResponseEntity.status(response.getStatusCode()).body(ApiError.of(BAD_REQUEST));
		}
		// "/api/trips/abc" where a number is expected: not ErrorResponse, still 400.
		if (ex instanceof TypeMismatchException) {
			return ResponseEntity.badRequest().body(ApiError.of(BAD_REQUEST));
		}
		log.error("Unhandled exception in an API call", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(SERVER_ERROR));
	}

}
