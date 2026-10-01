package com.voyagego.traingo.trip.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.voyagego.traingo.auth.model.User;
import com.voyagego.traingo.train.model.Seat;

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
 * The state of one seat on one trip. Created for every seat of the train when
 * the trip is created. The unique key on (trip_id, seat_id) is the last line
 * of defence against two bookings sharing a seat.
 */
@Entity
@Table(name = "trip_seats", uniqueConstraints = @UniqueConstraint(name = "uk_trip_seats_trip_seat",
		columnNames = { "trip_id", "seat_id" }))
public class TripSeat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private TripSeatStatus status;

	/** Who holds the seat while it is HELD. The database requires it then. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "held_by_user_id")
	private User heldBy;

	/** End of the 5 minute hold while the seat is HELD. */
	private LocalDateTime heldUntil;

	/** Required by JPA. */
	protected TripSeat() {
	}

	public TripSeat(Trip trip, Seat seat) {
		this.trip = trip;
		this.seat = seat;
		this.status = TripSeatStatus.AVAILABLE;
	}

	public Long getId() {
		return id;
	}

	public Trip getTrip() {
		return trip;
	}

	public Seat getSeat() {
		return seat;
	}

	public TripSeatStatus getStatus() {
		return status;
	}

	public User getHeldBy() {
		return heldBy;
	}

	public LocalDateTime getHeldUntil() {
		return heldUntil;
	}

}
