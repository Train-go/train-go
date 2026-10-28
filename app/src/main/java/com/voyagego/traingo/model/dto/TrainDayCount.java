package com.voyagego.traingo.model.dto;

import java.time.LocalDate;

/** Trips of one train on one day (cancelled ones left out), for the week grid. */
public interface TrainDayCount {

	long getTrainId();

	LocalDate getDay();

	long getTrips();

}
