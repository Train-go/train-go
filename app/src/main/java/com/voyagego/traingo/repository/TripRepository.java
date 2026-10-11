package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {

}
