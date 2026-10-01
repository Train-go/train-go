package com.voyagego.traingo.booking.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.voyagego.traingo.auth.model.User;
import com.voyagego.traingo.trip.model.Trip;

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
 * One customer's booking on one trip, for one or more passengers. Payment
 * state lives here; there is no separate Payment table. The database refuses
 * a CONFIRMED booking that is not PAID, and a PAID one without paidAt.
 */
@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(name = "uk_bookings_code",
		columnNames = "booking_code"))
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Shown to the customer, for example BK202610001. */
	@Column(nullable = false, length = 20)
	private String bookingCode;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	/** Sum of the passengers' ticket prices, in dong. */
	@Column(nullable = false)
	private long totalPrice;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private BookingStatus bookingStatus;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private PaymentStatus paymentStatus;

	private LocalDateTime paidAt;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	/** Required by JPA. */
	protected Booking() {
	}

	public Booking(String bookingCode, User customer, Trip trip, long totalPrice) {
		this.bookingCode = bookingCode;
		this.customer = customer;
		this.trip = trip;
		this.totalPrice = totalPrice;
		this.bookingStatus = BookingStatus.PENDING;
		this.paymentStatus = PaymentStatus.UNPAID;
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getBookingCode() {
		return bookingCode;
	}

	public User getCustomer() {
		return customer;
	}

	public Trip getTrip() {
		return trip;
	}

	public long getTotalPrice() {
		return totalPrice;
	}

	public BookingStatus getBookingStatus() {
		return bookingStatus;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
