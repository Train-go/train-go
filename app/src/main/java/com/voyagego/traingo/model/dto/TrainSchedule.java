package com.voyagego.traingo.model.dto;

import java.time.LocalDateTime;

/** What one train still has ahead of it: how many scheduled trips, the next and the last. */
public interface TrainSchedule {

	long getTrainId();

	long getUpcoming();

	LocalDateTime getNextAt();

	LocalDateTime getLastAt();

}
