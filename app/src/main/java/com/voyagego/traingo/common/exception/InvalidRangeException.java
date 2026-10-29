package com.voyagego.traingo.common.exception;

/** A date range asked for a chart is back to front or longer than the page allows. */
public class InvalidRangeException extends BusinessRuleException {

	public InvalidRangeException(String message) {
		super(message);
	}

}
