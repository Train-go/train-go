package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.model.dto.IdCount;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/**
	 * Every booking for the admin list, with customer, trip, train, route and
	 * both stations loaded. A null {@code customerId} means every customer.
	 * Grouping and searching happen in BookingAdminService.
	 */
	@Query("""
			select b from Booking b
			join fetch b.customer
			join fetch b.trip t
			join fetch t.train
			join fetch t.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where :customerId is null or b.customer.id = :customerId""")
	List<Booking> findAllForAdmin(@Param("customerId") Long customerId);

	/** One booking with the same relations as {@link #findAllForAdmin}. */
	@Query("""
			select b from Booking b
			join fetch b.customer
			join fetch b.trip t
			join fetch t.train
			join fetch t.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where b.bookingCode = :code""")
	Optional<Booking> findByBookingCode(@Param("code") String code);

	/** Number of bookings per customer id, cancelled ones included. */
	@Query("select b.customer.id as id, count(b) as total from Booking b group by b.customer.id")
	List<IdCount> countByCustomer();

	long countByTripIdAndBookingStatus(Long tripId, BookingStatus status);

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

	/** The id of the booking with this code, read without loading the booking itself. */
	@Query("select b.id from Booking b where b.bookingCode = :code")
	Optional<Long> findIdByBookingCode(@Param("code") String code);

	/**
	 * One booking, locked for writing. Cancelling takes this lock before it
	 * checks the booking, so two cancels of the same booking run one after the
	 * other and the second sees it already cancelled.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Booking b where b.id = :id")
	Optional<Booking> findByIdForUpdate(@Param("id") Long id);

}
