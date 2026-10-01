package com.voyagego.traingo.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.trip.model.TripSeat;

public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {

}
