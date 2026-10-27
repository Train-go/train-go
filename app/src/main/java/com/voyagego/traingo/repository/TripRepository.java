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

	/** Trips that are not cancelled, past ones included (the overview's "Chuyến"). */
	long countByStatusNot(TripStatus status);

	/** Trips of one status that leave after {@code after}; with SCHEDULED these are the upcoming ones (D8). */
	long countByStatusAndDepartureAtAfter(TripStatus status, LocalDateTime after);

	@Query("""
			select t.route.id as id, count(t) as total from Trip t
			where t.status = com.voyagego.traingo.entity.TripStatus.SCHEDULED
			and t.departureAt > :now group by t.route.id""")
	List<IdCount> countUpcomingByRoute(LocalDateTime now);

	/** Number of trips per route id, for routes that have at least one. */
	@Query("select t.route.id as id, count(t) as total from Trip t group by t.route.id")
	List<IdCount> countByRoute();

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

	/**
	 * Every trip of one train, cancelled ones included, earliest first, with
	 * the route and both stations loaded for the train page.
	 */
	@Query("""
			select t from Trip t
			join fetch t.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where t.train.id = :trainId
			order by t.departureAt""")
	List<Trip> findScheduleOfTrain(@Param("trainId") Long trainId);

	/** Called with CANCELLED: the last trip of the train's schedule. */
	@EntityGraph(attributePaths = { "route", "route.originStation", "route.destinationStation" })
	Optional<Trip> findFirstByTrainIdAndStatusNotOrderByDepartureAtDesc(Long trainId, TripStatus status);

	boolean existsByTrainId(Long trainId);

	/**
	 * Trips of the train that are not cancelled and have not arrived at
	 * {@code now}: upcoming and running ones. A train with any cannot be
	 * deleted; the page shows this same number next to the disabled button.
	 */
	@Query("""
			select count(t) from Trip t
			where t.train.id = :trainId
			and t.status <> com.voyagego.traingo.entity.TripStatus.CANCELLED
			and t.arrivalAt > :now""")
	long countUnfinishedByTrain(@Param("trainId") Long trainId, @Param("now") LocalDateTime now);

	/** One trip with its train, route and both stations loaded, for the admin's trip sheet. */
	@Query("""
			select t from Trip t
			join fetch t.train
			join fetch t.route r
			join fetch r.originStation
			join fetch r.destinationStation
			where t.id = :id""")
	Optional<Trip> findDetailById(@Param("id") Long id);

	/**
	 * Trips a customer can book from one station to another, leaving within
	 * [{@code dayStart}, {@code dayEnd}) and after {@code now} (D8), earliest
	 * first, with the train, route and both stations loaded for the trip cards.
	 */
	@Query("""
			select t from Trip t
			join fetch t.train
			join fetch t.route r
			join fetch r.originStation o
			join fetch r.destinationStation d
			where o.id = :originId and d.id = :destinationId
			and t.status = com.voyagego.traingo.entity.TripStatus.SCHEDULED
			and t.departureAt >= :dayStart and t.departureAt < :dayEnd
			and t.departureAt > :now
			order by t.departureAt""")
	List<Trip> findBookable(@Param("originId") Long originId, @Param("destinationId") Long destinationId,
			@Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd,
			@Param("now") LocalDateTime now);

}
