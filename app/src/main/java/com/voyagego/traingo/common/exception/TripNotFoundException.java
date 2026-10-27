package com.voyagego.traingo.common.exception;

public class TripNotFoundException extends ResourceNotFoundException {

	public TripNotFoundException(long id) {
		super("Không tìm thấy chuyến " + id + ".");
	}

}
