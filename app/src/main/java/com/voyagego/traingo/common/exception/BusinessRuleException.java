package com.voyagego.traingo.common.exception;

/**
 * The request is well formed but breaks a business rule: a code already in
 * use, a station still on a route, a seat already sold. The API answers 409
 * with this message, attached to one form field when {@link #getField()} names
 * it, so the page can show the message next to that field.
 *
 * The message is shown to the user, so it is written in Vietnamese.
 */
public abstract class BusinessRuleException extends RuntimeException {

	private final String field;

	protected BusinessRuleException(String message) {
		this(message, null);
	}

	protected BusinessRuleException(String message, String field) {
		super(message);
		this.field = field;
	}

	/** The request field the rule is about, or null when it is about the whole request. */
	public String getField() {
		return field;
	}

}
