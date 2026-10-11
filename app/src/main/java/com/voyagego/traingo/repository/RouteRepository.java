package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

	/** Whether any route starts or ends at the station. Pass its id twice. */
	boolean existsByOriginStationIdOrDestinationStationId(Long originStationId, Long destinationStationId);

}
