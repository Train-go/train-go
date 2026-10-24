package com.voyagego.traingo.common.exception;

public class StationNotFoundException extends ResourceNotFoundException {

	public StationNotFoundException() {
		super("Không tìm thấy ga này.");
	}

}
