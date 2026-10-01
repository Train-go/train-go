package com.voyagego.traingo.booking.model;

import java.time.LocalDate;

import com.voyagego.traingo.trip.model.TripSeat;

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
 * One traveller in a booking, sitting on exactly one TripSeat.
 *
 * A trip seat is unique within a booking but not across bookings: once a
 * booking is cancelled its seat goes back on sale and a later booking may use
 * it. That only one confirmed booking holds a seat is guarded by
 * TripSeat.status, not by a key here.
 */
@Entity
@Table(name = "booking_passengers", uniqueConstraints = @UniqueConstraint(name = "uk_booking_passengers_seat",
		columnNames = { "booking_id", "trip_seat_id" }))
public class BookingPassenger {

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

	/** Required by JPA. */
	protected BookingPassenger() {
	}

	public BookingPassenger(Booking booking, TripSeat tripSeat, String fullName, LocalDate dateOfBirth,
			String identityNumber, long ticketPrice) {
		this.booking = booking;
		this.tripSeat = tripSeat;
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.identityNumber = identityNumber;
		this.ticketPrice = ticketPrice;
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

}
