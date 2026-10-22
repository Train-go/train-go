package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.model.dto.IdCount;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	/** Number of passengers (tickets) per booking id. */
	@Query("select t.booking.id as id, count(t) as total from Ticket t group by t.booking.id")
	List<IdCount> countByBooking();

}
