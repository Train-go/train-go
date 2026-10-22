package com.voyagego.traingo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One run of a train on a route. A trip belongs to its train: the admin adds
 * it from the train's page, and it starts at the station where the train's
 * previous trip ended (docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md).
 *
 * Departure and arrival are full date-times, so a run of 24 hours or more,
 * such as Hà Nội - Sài Gòn, can be stored.
 */
@Entity
@Table(name = "trips")
public class Trip {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "train_id", nullable = false)
	private Train train;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private Route route;

	@Column(nullable = false)
	private LocalDateTime departureAt;

	@Column(nullable = false)
	private LocalDateTime arrivalAt;

	/** In dong. A ticket costs basePrice plus its trip seat's priceModifier. */
	@Column(nullable = false)
	private long basePrice;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private TripStatus status;

	/** Required by JPA. */
	protected Trip() {
	}

	public Trip(Train train, Route route, LocalDateTime departureAt, LocalDateTime arrivalAt, long basePrice) {
		this.train = train;
		this.route = route;
		this.departureAt = departureAt;
		this.arrivalAt = arrivalAt;
		this.basePrice = basePrice;
		this.status = TripStatus.SCHEDULED;
	}

	public Long getId() {
		return id;
	}

	public Train getTrain() {
		return train;
	}

	public Route getRoute() {
		return route;
	}

	public LocalDateTime getDepartureAt() {
		return departureAt;
	}

	public LocalDateTime getArrivalAt() {
		return arrivalAt;
	}

	public long getBasePrice() {
		return basePrice;
	}

	public TripStatus getStatus() {
		return status;
	}

}
