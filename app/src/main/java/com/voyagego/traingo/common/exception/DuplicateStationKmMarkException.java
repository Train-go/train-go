package com.voyagego.traingo.common.exception;

import com.voyagego.traingo.entity.Station;

public class DuplicateStationKmMarkException extends BusinessRuleException {

	public DuplicateStationKmMarkException(int kmMark, Station station) {
		super("Km " + kmMark + " đã là lý trình của " + station.getName() + ".", "kmMark");
	}

}
