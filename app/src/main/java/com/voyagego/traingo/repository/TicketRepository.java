package com.voyagego.traingo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.model.dto.IdCount;

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

}
