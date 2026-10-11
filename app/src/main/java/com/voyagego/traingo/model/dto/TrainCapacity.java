package com.voyagego.traingo.model.dto;

/**
 * How many coaches a train has and how many seats they add up to. Filled from
 * the aliases {@code trainId}, {@code coachCount} and {@code seatCount} of a
 * JPQL select.
 */
public interface TrainCapacity {

	Long getTrainId();

	long getCoachCount();

	long getSeatCount();

}
