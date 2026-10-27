package com.voyagego.traingo.common.exception;

/**
 * The chosen seats break a rule the seat page already prevents: none chosen,
 * more than 10, a seat twice or a seat of another trip. Only a hand-built
 * request reaches it, so the sentence is plain.
 */
public class SeatSelectionException extends BusinessRuleException {

	public SeatSelectionException(String message) {
		super(message);
	}

}
