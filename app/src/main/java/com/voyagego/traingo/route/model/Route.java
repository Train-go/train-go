package com.voyagego.traingo.route.model;

import com.voyagego.traingo.station.model.Station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A direct connection between two stations, with no stations in between
 * (docs/product/domain-model.md). The database refuses a route whose two ends
 * are the same station, and a second route between the same pair.
 */
@Entity
@Table(name = "routes", uniqueConstraints = @UniqueConstraint(name = "uk_routes_origin_destination",
		columnNames = { "origin_station_id", "destination_station_id" }))
public class Route {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "origin_station_id", nullable = false)
	private Station originStation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_station_id", nullable = false)
	private Station destinationStation;

	@Column(nullable = false)
	private int distanceKm;

	/** Required by JPA. */
	protected Route() {
	}

	public Route(Station originStation, Station destinationStation, int distanceKm) {
		this.originStation = originStation;
		this.destinationStation = destinationStation;
		this.distanceKm = distanceKm;
	}

	public Long getId() {
		return id;
	}

	public Station getOriginStation() {
		return originStation;
	}

	public Station getDestinationStation() {
		return destinationStation;
	}

	public int getDistanceKm() {
		return distanceKm;
	}

}
