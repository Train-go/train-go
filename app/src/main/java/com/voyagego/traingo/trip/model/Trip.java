package com.voyagego.traingo.trip.model;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.voyagego.traingo.route.model.Route;
import com.voyagego.traingo.train.model.Train;

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
 * One run of a train on a route on a given day.
 *
 * SPEC.md gives a trip a departure date and two times, with no arrival date.
 * An arrival time earlier than the departure time therefore means the next
 * day, and a trip cannot last 24 hours or more.
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
	private LocalDate departureDate;

	@Column(nullable = false)
	private LocalTime departureTime;

	@Column(nullable = false)
	private LocalTime arrivalTime;

	/** In dong. A ticket costs basePrice plus its coach's priceModifier. */
	@Column(nullable = false)
	private long basePrice;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private TripStatus status;

	/** Required by JPA. */
	protected Trip() {
	}

	public Trip(Train train, Route route, LocalDate departureDate, LocalTime departureTime, LocalTime arrivalTime,
			long basePrice) {
		this.train = train;
		this.route = route;
		this.departureDate = departureDate;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
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

	public LocalDate getDepartureDate() {
		return departureDate;
	}

	public LocalTime getDepartureTime() {
		return departureTime;
	}

	public LocalTime getArrivalTime() {
		return arrivalTime;
	}

	public long getBasePrice() {
		return basePrice;
	}

	public TripStatus getStatus() {
		return status;
	}

}
