package com.voyagego.traingo.common.exception;

public class DuplicateStationCodeException extends BusinessRuleException {

	public DuplicateStationCodeException() {
		super("Mã ga này đã được dùng cho một ga khác.", "code");
	}

}
