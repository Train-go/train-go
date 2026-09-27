package com.voyagego.traingo.admin.model;

public class Booking {

	private final String code;
	private final String customer;
	private final String trip;
	private final int passengers;
	private final long total;
	private final String paymentStatus;

	public Booking(String code, String customer, String trip, int passengers, long total, String paymentStatus) {
		this.code = code;
		this.customer = customer;
		this.trip = trip;
		this.passengers = passengers;
		this.total = total;
		this.paymentStatus = paymentStatus;
	}

	public String getCode() {
		return code;
	}

	public String getCustomer() {
		return customer;
	}

	public String getTrip() {
		return trip;
	}

	public int getPassengers() {
		return passengers;
	}

	public long getTotal() {
		return total;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

}
