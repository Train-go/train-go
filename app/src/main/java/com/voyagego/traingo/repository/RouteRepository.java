package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

	/** Whether any route starts or ends at the station. Pass its id twice. */
	boolean existsByOriginStationIdOrDestinationStationId(Long originStationId, Long destinationStationId);

	/**
	 * Every route with both stations loaded, so a view can print their names.
	 * Sorted along the line: by where it starts, then by where it ends.
	 */
	@Query("""
			select r from Route r join fetch r.originStation o join fetch r.destinationStation d
			order by o.kmMark, d.kmMark""")
	List<Route> findAllWithStations();

}
