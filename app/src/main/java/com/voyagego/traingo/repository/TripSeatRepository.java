package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.TripSeat;

public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {

	/** The trip's seat map, coach by coach. */
	List<TripSeat> findByTripIdOrderByCoachNumberAscSeatCodeAsc(Long tripId);

}
