package com.voyagego.traingo.ticket.model;

import java.time.LocalDateTime;

import com.voyagego.traingo.booking.model.BookingPassenger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The e-ticket of one passenger, issued when the booking is paid. Route,
 * train, departure, coach and seat are read through the passenger's trip seat
 * rather than copied here.
 */
@Entity
@Table(name = "tickets", uniqueConstraints = {
		@UniqueConstraint(name = "uk_tickets_code", columnNames = "ticket_code"),
		@UniqueConstraint(name = "uk_tickets_passenger", columnNames = "booking_passenger_id") })
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** For example TK202610001-01: the booking code, then the passenger. */
	@Column(nullable = false, length = 20)
	private String ticketCode;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_passenger_id", nullable = false)
	private BookingPassenger bookingPassenger;

	@Column(nullable = false)
	private LocalDateTime issuedAt;

	/** Required by JPA. */
	protected Ticket() {
	}

	public Ticket(String ticketCode, BookingPassenger bookingPassenger) {
		this.ticketCode = ticketCode;
		this.bookingPassenger = bookingPassenger;
		this.issuedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getTicketCode() {
		return ticketCode;
	}

	public BookingPassenger getBookingPassenger() {
		return bookingPassenger;
	}

	public LocalDateTime getIssuedAt() {
		return issuedAt;
	}

}
