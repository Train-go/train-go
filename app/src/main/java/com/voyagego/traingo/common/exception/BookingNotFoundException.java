package com.voyagego.traingo.common.exception;

public class BookingNotFoundException extends ResourceNotFoundException {

	public BookingNotFoundException() {
		super("Không tìm thấy booking.");
	}

}
