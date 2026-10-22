package com.voyagego.traingo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;


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
 * One traveller in a booking, sitting on exactly one TripSeat, and the
 * e-ticket for that seat. Route, train, departure, coach and seat are read
 * through the trip seat rather than copied here.
 *
 * A row is created inside the payment transaction, together with its booking,
 * so it always has a ticketCode and an issuedAt.
 *
 * A trip seat is unique within a booking but not across bookings: once a
 * booking is cancelled its seat goes back on sale and a later booking may use
 * it. That only one confirmed booking holds a seat is guarded by
 * TripSeat.status, not by a key here.
 */
@Entity
@Table(name = "tickets", uniqueConstraints = {
		@UniqueConstraint(name = "uk_tickets_seat", columnNames = { "booking_id", "trip_seat_id" }),
		@UniqueConstraint(name = "uk_tickets_code", columnNames = "ticket_code") })
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_seat_id", nullable = false)
	private TripSeat tripSeat;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false)
	private LocalDate dateOfBirth;

	/** CCCD or passport number. Never put it in a QR code. */
	@Column(nullable = false, length = 20)
	private String identityNumber;

	/**
	 * Price when booked, in dong. Frozen here so a later change to the trip's
	 * basePrice does not rewrite old bookings.
	 */
	@Column(nullable = false)
	private long ticketPrice;

	/** For example TK202610001-01: the booking code, then the passenger. */
	@Column(nullable = false, length = 20)
	private String ticketCode;

	@Column(nullable = false)
	private LocalDateTime issuedAt;

	/** Required by JPA. */
	protected Ticket() {
	}

	public Ticket(Booking booking, TripSeat tripSeat, String fullName, LocalDate dateOfBirth,
			String identityNumber, long ticketPrice, String ticketCode) {
		this.booking = booking;
		this.tripSeat = tripSeat;
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.identityNumber = identityNumber;
		this.ticketPrice = ticketPrice;
		this.ticketCode = ticketCode;
		this.issuedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Booking getBooking() {
		return booking;
	}

	public TripSeat getTripSeat() {
		return tripSeat;
	}

	public String getFullName() {
		return fullName;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public String getIdentityNumber() {
		return identityNumber;
	}

	public long getTicketPrice() {
		return ticketPrice;
	}

	public String getTicketCode() {
		return ticketCode;
	}

	public LocalDateTime getIssuedAt() {
		return issuedAt;
	}

}
