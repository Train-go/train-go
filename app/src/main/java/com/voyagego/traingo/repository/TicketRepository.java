package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

}
