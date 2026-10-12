package com.voyagego.traingo.common.exception;

public class DuplicateStationCodeException extends RuntimeException {

	public DuplicateStationCodeException(String code) {
		super("Station code " + code + " is already in use");
	}

}
