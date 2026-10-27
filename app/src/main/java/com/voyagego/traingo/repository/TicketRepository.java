package com.voyagego.traingo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

	/** Tickets of several bookings, with the seat loaded so a view can print coach and seat. */
	@Query("""
			select t from Ticket t join fetch t.tripSeat
			where t.booking.id in :ids
			order by t.ticketCode""")
	List<Ticket> findByBookingIds(@Param("ids") Collection<Long> ids);

	/** Tickets of one trip that belong to a confirmed booking, with booking and seat loaded. */
	@Query("""
			select t from Ticket t
			join fetch t.booking b
			join fetch t.tripSeat s
			where s.trip.id = :tripId
			and b.bookingStatus = com.voyagego.traingo.entity.BookingStatus.CONFIRMED
			order by s.coachNumber, s.seatCode""")
	List<Ticket> findConfirmedByTrip(@Param("tripId") Long tripId);

	/** Sold seats per trip id, for trips that have at least one. */
	@Query("""
			select t.tripSeat.trip.id as id, count(t) as total from Ticket t
			where t.booking.bookingStatus = com.voyagego.traingo.entity.BookingStatus.CONFIRMED
			group by t.tripSeat.trip.id""")
	List<IdCount> countConfirmedByTrip();

	/**
	 * One ticket with everything the e-ticket prints loaded: booking, customer,
	 * seat, trip, train, route and both stations.
	 */
	@Query("""
			select t from Ticket t
			join fetch t.booking b
			join fetch b.customer
			join fetch t.tripSeat s
			join fetch s.trip trip
			join fetch trip.train
			join fetch trip.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where t.id = :id""")
	Optional<Ticket> findDetailById(@Param("id") Long id);

	/** The tickets of one booking, loaded like {@link #findDetailById}, in code order (-01, -02 ...). */
	@Query("""
			select t from Ticket t
			join fetch t.booking b
			join fetch b.customer
			join fetch t.tripSeat s
			join fetch s.trip trip
			join fetch trip.train
			join fetch trip.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where b.id = :bookingId
			order by t.ticketCode""")
	List<Ticket> findDetailsByBookingId(@Param("bookingId") Long bookingId);

}
