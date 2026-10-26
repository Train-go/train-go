package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.dto.IdCount;

public interface TripRepository extends JpaRepository<Trip, Long> {
	long countByRouteId(Long routeId);

	@Query("""
			select t.route.id as id, count(t) as total from Trip t
			where t.status = com.voyagego.traingo.entity.TripStatus.SCHEDULED
			and t.departureAt > :now group by t.route.id""")
	List<IdCount> countUpcomingByRoute(LocalDateTime now);

	/** Every trip, earliest first, with its train, route and both stations loaded. */
	@EntityGraph(attributePaths = { "train", "route", "route.originStation", "route.destinationStation" })
	List<Trip> findAllByOrderByDepartureAtAsc();

	/** Number of trips per route id, for routes that have at least one. */
	@Query("select t.route.id as id, count(t) as total from Trip t group by t.route.id")
	List<IdCount> countByRoute();

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

}
