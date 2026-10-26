package com.voyagego.traingo.common.exception;

public class StationCodeLockedException extends BusinessRuleException {

	public StationCodeLockedException() {
		super("Ga đã có tuyến nên giữ mã.", "code");
	}

}
