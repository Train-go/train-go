package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.model.dto.IdCount;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/** Newest first, with customer, trip and train loaded for the admin list. */
	@EntityGraph(attributePaths = { "customer", "trip", "trip.train" })
	List<Booking> findAllByOrderByCreatedAtDesc();

	@Query("select coalesce(sum(b.totalPrice), 0) from Booking b where b.bookingStatus = :status")
	long sumTotalPriceByBookingStatus(@Param("status") BookingStatus status);

	/** Number of bookings per route id, leaving out bookings with the given status. */
	@Query("""
			select b.trip.route.id as id, count(b) as total
			from Booking b
			where b.bookingStatus <> :excluded
			group by b.trip.route.id
			""")
	List<IdCount> countByRouteExcludingStatus(@Param("excluded") BookingStatus excluded);

}
