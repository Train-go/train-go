package com.voyagego.traingo.model.response;

import java.util.Map;

/**
 * The one shape every @RestController error takes, read by TG.api in
 * static/js/admin/admin.js and by the customer pages' scripts:
 *
 * <pre>{ "message": "...", "fields": { "code": "..." } }</pre>
 *
 * {@code message} is Vietnamese and shown as is. {@code fields} maps a request
 * field to its message so the page can show it next to that input; it is an
 * empty object, never missing, when the error is about the whole request.
 */
public record ApiError(String message, Map<String, String> fields) {

	public ApiError {
		fields = fields == null ? Map.of() : Map.copyOf(fields);
	}

	public static ApiError of(String message) {
		return new ApiError(message, Map.of());
	}

}
