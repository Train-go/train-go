package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.model.dto.IdCount;

public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {

	/** Booked seats per trip of one train, for trips that have at least one. */
	@Query("""
			select s.trip.id as id, count(s) as total from TripSeat s
			where s.trip.train.id = :trainId
			and s.status = com.voyagego.traingo.entity.TripSeatStatus.BOOKED
			group by s.trip.id""")
	List<IdCount> countBookedByTrain(@Param("trainId") Long trainId);

	/** Every seat of one trip, in coach then seat-code order. */
	List<TripSeat> findByTripIdOrderByCoachNumberAscSeatCodeAsc(Long tripId);

}
