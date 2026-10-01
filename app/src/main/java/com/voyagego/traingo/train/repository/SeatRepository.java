package com.voyagego.traingo.train.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.train.model.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

}
