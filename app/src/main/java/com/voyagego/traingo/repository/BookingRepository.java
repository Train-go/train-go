package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

}
