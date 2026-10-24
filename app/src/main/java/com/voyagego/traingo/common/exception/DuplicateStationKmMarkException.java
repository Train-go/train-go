package com.voyagego.traingo.common.exception;

public class DuplicateStationKmMarkException extends BusinessRuleException {

	public DuplicateStationKmMarkException() {
		super("Lý trình này đã được dùng cho một ga khác.", "kmMark");
	}

}
