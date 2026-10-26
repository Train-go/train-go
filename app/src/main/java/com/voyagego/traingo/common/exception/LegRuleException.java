package com.voyagego.traingo.common.exception;

/**
 * One leg of a batch of trips breaks a rule: no route between its stations, a
 * departure before the train can leave, an arrival before the departure, a
 * price that is not a whole number of thousands. The API answers 409 with the
 * message under {@code legs[index].field}.
 */
public class LegRuleException extends BusinessRuleException {

	public LegRuleException(int legIndex, String field, String message) {
		super(message, "legs[" + legIndex + "]." + field);
	}

}
