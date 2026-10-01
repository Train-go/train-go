package com.voyagego.traingo.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.trip.model.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {

}
