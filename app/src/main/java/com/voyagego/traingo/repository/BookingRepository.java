package com.voyagego.traingo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.model.dto.DayTotal;
import com.voyagego.traingo.model.dto.IdCount;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/**
	 * Every booking for the admin list, with customer, trip, train, route and
	 * both stations loaded. A null {@code customerId} means every customer.
	 * Grouping and searching happen in BookingAdminService; MyTripsService
	 * reads one customer's bookings through the same query.
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

	/**
	 * One booking of one customer, with the same relations as
	 * {@link #findAllForAdmin}. Empty for another customer's booking, so My
	 * Trips cannot tell it apart from one that does not exist.
	 */
	@Query("""
			select b from Booking b
			join fetch b.customer
			join fetch b.trip t
			join fetch t.train
			join fetch t.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where b.id = :id and b.customer.id = :customerId""")
	Optional<Booking> findOwnedById(@Param("id") Long id, @Param("customerId") Long customerId);

	/** Ownership check that loads nothing; see MyTripsService.cancel for why. */
	boolean existsByIdAndCustomerId(Long id, Long customerId);

	/** Number of bookings per customer id, cancelled ones included. */
	@Query("select b.customer.id as id, count(b) as total from Booking b group by b.customer.id")
	List<IdCount> countByCustomer();

	long countByTripIdAndBookingStatus(Long tripId, BookingStatus status);

	/** Bookings in one status, for the overview's totals. */
	long countByBookingStatus(BookingStatus status);

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

	/**
	 * The greatest booking code that starts with the prefix, such as
	 * "BK202610", or empty when the month has none. Codes in a month end in
	 * three digits, so string order is number order (BookingCodeGenerator).
	 */
	@Query("select max(b.bookingCode) from Booking b where b.bookingCode like concat(:prefix, '%')")
	Optional<String> findMaxCodeWithPrefix(@Param("prefix") String prefix);

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

	/**
	 * Confirmed bookings per day they were paid, in [from, to). Native SQL for
	 * the same reason as TripRepository.findDayPrices: JPQL cannot group by the
	 * date of a timestamp.
	 */
	@Query(value = """
			select date(b.paid_at) as day, count(*) as bookings, coalesce(sum(b.total_price), 0) as revenue
			from bookings b
			where b.booking_status = 'CONFIRMED' and b.paid_at >= :from and b.paid_at < :to
			group by date(b.paid_at)
			order by day""", nativeQuery = true)
	List<DayTotal> findSalesByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}
