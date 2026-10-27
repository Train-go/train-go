package com.voyagego.traingo.model.dto;

import java.io.Serializable;
import java.util.List;

/**
 * The booking in progress, kept in the session until payment (D7 in
 * docs/product/booking-flow.md). Nothing here is in the database.
 *
 * {@code passengers} lines up with {@code tripSeatIds}: passenger i sits in
 * seat i, in the order the customer chose the seats.
 */
public final class BookingDraft implements Serializable {

	private static final long serialVersionUID = 1L;

	/** One passenger line. The passenger step fills the identity number. */
	public record PassengerDraft(String fullName, String identityNumber) implements Serializable {
	}

	private final Long tripId;
	private final List<Long> tripSeatIds;
	private final List<PassengerDraft> passengers;

	public BookingDraft(Long tripId, List<Long> tripSeatIds, List<PassengerDraft> passengers) {
		this.tripId = tripId;
		this.tripSeatIds = List.copyOf(tripSeatIds);
		this.passengers = List.copyOf(passengers);
	}

	public Long getTripId() {
		return tripId;
	}

	public List<Long> getTripSeatIds() {
		return tripSeatIds;
	}

	public List<PassengerDraft> getPassengers() {
		return passengers;
	}

}
