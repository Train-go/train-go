package com.voyagego.traingo.common.exception;

public class TripNotFoundException extends ResourceNotFoundException {

	public TripNotFoundException() {
		super("Không tìm thấy chuyến này.");
	}

}
