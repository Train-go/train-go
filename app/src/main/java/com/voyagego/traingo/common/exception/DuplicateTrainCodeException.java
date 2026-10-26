package com.voyagego.traingo.common.exception;

public class DuplicateTrainCodeException extends BusinessRuleException {

	public DuplicateTrainCodeException() {
		super("Mã này đã có tàu khác dùng.", "code");
	}

}
