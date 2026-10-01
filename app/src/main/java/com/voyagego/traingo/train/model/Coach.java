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
 * One coach of a train. Its seats are generated from coachType and capacity
 * when the coach is added, never entered one by one.
 */
@Entity
@Table(name = "coaches", uniqueConstraints = @UniqueConstraint(name = "uk_coaches_train_number",
		columnNames = { "train_id", "coach_number" }))
public class Coach {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "train_id", nullable = false)
	private Train train;

	/** 1, 2, 3 ... within its train; shown as "Toa 01". */
	@Column(nullable = false)
	private int coachNumber;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private CoachType coachType;

	@Column(nullable = false)
	private int capacity;

	/** Added to Trip.basePrice for every seat in this coach, in dong. */
	@Column(nullable = false)
	private long priceModifier;

	/** Required by JPA. */
	protected Coach() {
	}

	public Coach(Train train, int coachNumber, CoachType coachType, int capacity, long priceModifier) {
		this.train = train;
		this.coachNumber = coachNumber;
		this.coachType = coachType;
		this.capacity = capacity;
		this.priceModifier = priceModifier;
	}

	public Long getId() {
		return id;
	}

	public Train getTrain() {
		return train;
	}

	public int getCoachNumber() {
		return coachNumber;
	}

	public CoachType getCoachType() {
		return coachType;
	}

	public int getCapacity() {
		return capacity;
	}

	public long getPriceModifier() {
		return priceModifier;
	}

}
