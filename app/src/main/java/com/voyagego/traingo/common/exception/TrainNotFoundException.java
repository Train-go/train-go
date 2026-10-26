package com.voyagego.traingo.common.exception;

public class TrainNotFoundException extends ResourceNotFoundException {

	public TrainNotFoundException() {
		super("Không tìm thấy tàu này.");
	}

}
