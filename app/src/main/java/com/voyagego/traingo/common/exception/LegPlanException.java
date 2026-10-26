package com.voyagego.traingo.common.exception;

/**
 * A multi-leg plan cannot be made: some pair of neighbouring stations on the
 * way has no route, or the two ends are the same station. Answered with 409
 * and the message below.
 */
public class LegPlanException extends BusinessRuleException {

	public LegPlanException(String message) {
		super(message);
	}

	public static LegPlanException missingRoute() {
		return new LegPlanException("Thiếu tuyến giữa hai ga kề nhau trên đường đi.");
	}

	public static LegPlanException sameStation() {
		return new LegPlanException("Ga cuối phải khác ga đang đứng.");
	}

}
