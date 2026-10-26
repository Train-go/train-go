package com.voyagego.traingo.common.exception;

public class NoFreeKmException extends BusinessRuleException {

	public NoFreeKmException() {
		super("Không còn km trống giữa hai ga.");
	}

}
