package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

}
