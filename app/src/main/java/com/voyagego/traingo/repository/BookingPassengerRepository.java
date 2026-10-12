package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.BookingPassenger;
import com.voyagego.traingo.model.dto.IdCount;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {

	/** Number of passengers per booking id. */
	@Query("select bp.booking.id as id, count(bp) as total from BookingPassenger bp group by bp.booking.id")
	List<IdCount> countByBooking();

}
