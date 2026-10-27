package com.voyagego.traingo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.model.dto.IdCount;
import com.voyagego.traingo.model.dto.SeatAvailability;

public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {

	/** Booked seats per trip of one train, for trips that have at least one. */
	@Query("""
			select s.trip.id as id, count(s) as total from TripSeat s
			where s.trip.train.id = :trainId
			and s.status = com.voyagego.traingo.entity.TripSeatStatus.BOOKED
			group by s.trip.id""")
	List<IdCount> countBookedByTrain(@Param("trainId") Long trainId);

	/**
	 * Free seats per trip and coach type, for every trip of a result page in
	 * one query. A trip or type with no free seat has no row.
	 */
	@Query("""
			select ts.trip.id as tripId, ts.coachType as coachType, count(ts) as available from TripSeat ts
			where ts.trip.id in :tripIds
			and ts.status = com.voyagego.traingo.entity.TripSeatStatus.AVAILABLE
			group by ts.trip.id, ts.coachType""")
	List<SeatAvailability> countAvailableByTripAndType(@Param("tripIds") Collection<Long> tripIds);

	/** Every seat of one trip, in coach then seat-code order. */
	List<TripSeat> findByTripIdOrderByCoachNumberAscSeatCodeAsc(Long tripId);

	/** Puts the seats of a booking back on sale; returns how many it released. */
	@Modifying
	@Query("""
			update TripSeat ts set ts.status = com.voyagego.traingo.entity.TripSeatStatus.AVAILABLE
			where ts.status = com.voyagego.traingo.entity.TripSeatStatus.BOOKED
			and ts.id in (select t.tripSeat.id from Ticket t where t.booking.id = :bookingId)""")
	int releaseSeatsOfBooking(@Param("bookingId") Long bookingId);

}
