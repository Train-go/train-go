package com.voyagego.traingo.common.exception;

/** A batch of route ids to delete is empty or longer than the limit. */
public class InvalidRouteBatchException extends BusinessRuleException {

	public InvalidRouteBatchException(String message) {
		super(message);
	}

}
