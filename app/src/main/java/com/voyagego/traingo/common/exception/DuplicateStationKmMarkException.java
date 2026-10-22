package com.voyagego.traingo.common.exception;

public class DuplicateStationKmMarkException extends RuntimeException {

	public DuplicateStationKmMarkException(int kmMark) {
		super("Km mark " + kmMark + " is already used by another station");
	}

}
