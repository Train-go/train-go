package com.voyagego.traingo.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.booking.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

}
