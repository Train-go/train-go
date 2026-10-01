package com.voyagego.traingo.train.model;

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
import jakarta.persistence.UniqueConstraint;

/**
 * A physical seat or bed in a coach. Whether it is free on a given day is
 * TripSeat's business, not this class's.
 */
@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(name = "uk_seats_coach_code",
		columnNames = { "coach_id", "code" }))
public class Seat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coach_id", nullable = false)
	private Coach coach;

	/** A01 ... for seats, B01 ... for beds. */
	@Column(nullable = false, length = 5)
	private String code;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 10)
	private SeatType type;

	/** Beds only: four beds per cabin, so B01-B04 are cabin 1. Null for seats. */
	private Integer cabinNumber;

	/** Required by JPA. */
	protected Seat() {
	}

	public Seat(Coach coach, String code, SeatType type, Integer cabinNumber) {
		this.coach = coach;
		this.code = code;
		this.type = type;
		this.cabinNumber = cabinNumber;
	}

	public Long getId() {
		return id;
	}

	public Coach getCoach() {
		return coach;
	}

	public String getCode() {
		return code;
	}

	public SeatType getType() {
		return type;
	}

	public Integer getCabinNumber() {
		return cabinNumber;
	}

}
