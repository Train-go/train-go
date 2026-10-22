package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.model.dto.IdCount;

public interface TripRepository extends JpaRepository<Trip, Long> {

	/** Every trip, earliest first, with its train, route and both stations loaded. */
	@EntityGraph(attributePaths = { "train", "route", "route.originStation", "route.destinationStation" })
	List<Trip> findAllByOrderByDepartureAtAsc();

	/** Number of trips per route id, for routes that have at least one. */
	@Query("select t.route.id as id, count(t) as total from Trip t group by t.route.id")
	List<IdCount> countByRoute();

}
