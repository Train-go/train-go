package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

	/** Whether any route starts or ends at the station. Pass its id twice. */
	boolean existsByOriginStationIdOrDestinationStationId(Long originStationId, Long destinationStationId);

	/** Every route with both stations loaded, so a view can print their names. */
	@Query("select r from Route r join fetch r.originStation join fetch r.destinationStation order by r.id")
	List<Route> findAllWithStations();

}
