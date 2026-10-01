package com.voyagego.traingo.station.service;

public class DuplicateStationCodeException extends RuntimeException {

	public DuplicateStationCodeException(String code) {
		super("Station code " + code + " is already in use");
	}

}
