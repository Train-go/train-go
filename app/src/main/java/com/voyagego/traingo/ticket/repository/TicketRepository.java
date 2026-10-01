package com.voyagego.traingo.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.ticket.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

}
