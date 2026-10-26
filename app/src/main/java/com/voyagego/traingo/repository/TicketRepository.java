package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.model.dto.IdCount;

/**
 * A seat counts as sold when a ticket of a CONFIRMED booking sits on it. The
 * trip sheet and the trip list both count that way, so their numbers agree.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

	/** Number of passengers (tickets) per booking id. */
	@Query("select t.booking.id as id, count(t) as total from Ticket t group by t.booking.id")
	List<IdCount> countByBooking();

	/** Tickets of the trip's confirmed bookings, with booking and seat loaded. */
	@Query("""
			select t from Ticket t join fetch t.booking b join fetch t.tripSeat s
			where s.trip.id = :tripId
			and b.bookingStatus = com.voyagego.traingo.entity.BookingStatus.CONFIRMED""")
	List<Ticket> findConfirmedByTrip(@Param("tripId") Long tripId);

	/** Sold seats per trip id, for trips that have at least one. */
	@Query("""
			select t.tripSeat.trip.id as id, count(t) as total from Ticket t
			where t.booking.bookingStatus = com.voyagego.traingo.entity.BookingStatus.CONFIRMED
			group by t.tripSeat.trip.id""")
	List<IdCount> countConfirmedByTrip();

}
