package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.dto.IdCount;

import jakarta.persistence.LockModeType;

/**
 * The three trip groups of /admin/trips follow D8: a trip is upcoming while it
 * is SCHEDULED and departs after now, departed once its departure is not after
 * now, and cancelled when CANCELLED. Each group has a list and a count query
 * with the same condition, so the counts on the page match the rows.
 */
public interface TripRepository extends JpaRepository<Trip, Long> {
	long countByRouteId(Long routeId);

	@Query("""
			select t.route.id as id, count(t) as total from Trip t
			where t.status = com.voyagego.traingo.entity.TripStatus.SCHEDULED
			and t.departureAt > :now group by t.route.id""")
	List<IdCount> countUpcomingByRoute(LocalDateTime now);

	/** Number of trips per route id, for routes that have at least one. */
	@Query("select t.route.id as id, count(t) as total from Trip t group by t.route.id")
	List<IdCount> countByRoute();

	/** One trip with its train, route and both stations loaded, for the trip sheet. */
	@EntityGraph(attributePaths = { "train", "route", "route.originStation", "route.destinationStation" })
	Optional<Trip> findWithDetailsById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Trip t where t.id = :id")
	Optional<Trip> findByIdForUpdate(@Param("id") Long id);

	/** {@code trainId} null means every train. */
	@Query("""
			select t from Trip t join fetch t.train join fetch t.route r
			join fetch r.originStation join fetch r.destinationStation
			where t.status = com.voyagego.traingo.entity.TripStatus.SCHEDULED and t.departureAt > :now
			and (:trainId is null or t.train.id = :trainId)""")
	List<Trip> findUpcoming(@Param("now") LocalDateTime now, @Param("trainId") Long trainId, Sort sort);

	/** {@code trainId} null means every train. */
	@Query("""
			select t from Trip t join fetch t.train join fetch t.route r
			join fetch r.originStation join fetch r.destinationStation
			where t.status <> com.voyagego.traingo.entity.TripStatus.CANCELLED and t.departureAt <= :now
			and (:trainId is null or t.train.id = :trainId)""")
	List<Trip> findDeparted(@Param("now") LocalDateTime now, @Param("trainId") Long trainId, Sort sort);

	/** {@code trainId} null means every train. */
	@Query("""
			select t from Trip t join fetch t.train join fetch t.route r
			join fetch r.originStation join fetch r.destinationStation
			where t.status = com.voyagego.traingo.entity.TripStatus.CANCELLED
			and (:trainId is null or t.train.id = :trainId)""")
	List<Trip> findCancelled(@Param("trainId") Long trainId, Sort sort);

	@Query("""
			select count(t) from Trip t
			where t.status = com.voyagego.traingo.entity.TripStatus.SCHEDULED and t.departureAt > :now""")
	long countUpcoming(@Param("now") LocalDateTime now);

	@Query("""
			select count(t) from Trip t
			where t.status <> com.voyagego.traingo.entity.TripStatus.CANCELLED and t.departureAt <= :now""")
	long countDeparted(@Param("now") LocalDateTime now);

	long countByStatus(TripStatus status);

}
