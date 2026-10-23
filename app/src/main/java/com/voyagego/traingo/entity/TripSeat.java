package com.voyagego.traingo.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.voyagego.traingo.model.dto.CoachLayout;

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
import jakarta.persistence.UniqueConstraint;

/**
 * One seat or bed on one trip, with its own state. Created for every seat of
 * the fixed coach layout when the trip is created (docs/product/domain-model.md
 * describes the layout). The coach and seat data is copied here, so a trip's
 * seat map reads from this one table. The unique key on
 * (trip_id, coach_number, seat_code) is the last line of defence against two
 * bookings sharing a seat.
 */
@Entity
@Table(name = "trip_seats", uniqueConstraints = @UniqueConstraint(name = "uk_trip_seats_coach_seat",
		columnNames = { "trip_id", "coach_number", "seat_code" }))
public class TripSeat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	/** 1, 2, 3 ... within the trip; shown as "Toa 01". */
	@Column(nullable = false)
	private int coachNumber;

	/** SEAT coaches hold seats A01 ..., SLEEPER coaches hold beds B01 ... */
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private CoachType coachType;

	/** A01 ... for seats, B01 ... for beds. */
	@Column(nullable = false, length = 5)
	private String seatCode;

	/** Beds only: four beds per cabin, so B01-B04 are cabin 1. Null for seats. */
	private Integer cabinNumber;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private TripSeatStatus status;

	/** Required by JPA. */
	protected TripSeat() {
	}

	public TripSeat(Trip trip, int coachNumber, CoachType coachType, String seatCode, Integer cabinNumber) {
		this.trip = trip;
		this.coachNumber = coachNumber;
		this.coachType = coachType;
		this.seatCode = seatCode;
		this.cabinNumber = cabinNumber;
		this.status = TripSeatStatus.AVAILABLE;
	}

	public TripSeat(Trip trip, CoachLayout.Seat seat) {
		this(trip, seat.coachNumber(), seat.type(), seat.seatCode(), seat.cabinNumber());
	}

	public Long getId() {
		return id;
	}

	public Trip getTrip() {
		return trip;
	}

	public int getCoachNumber() {
		return coachNumber;
	}

	public CoachType getCoachType() {
		return coachType;
	}

	public String getSeatCode() {
		return seatCode;
	}

	public Integer getCabinNumber() {
		return cabinNumber;
	}

	public TripSeatStatus getStatus() {
		return status;
	}

}
