package com.voyagego.traingo.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.booking.model.BookingPassenger;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {

}
