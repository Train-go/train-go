package com.voyagego.traingo.model.dto;

import java.time.LocalDate;

/** How many rows fall on one day, for a chart. A day with none has no row. */
public interface DayCount {

	LocalDate getDay();

	long getTotal();

}
