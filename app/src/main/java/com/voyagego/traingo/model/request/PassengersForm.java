package com.voyagego.traingo.model.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

/**
 * The passenger page's form. Line i belongs to seat i of the booking draft,
 * so the list keeps the order the customer chose the seats in.
 */
public class PassengersForm {

	@Valid
	private List<PassengerRequest> passengers = new ArrayList<>();

	public List<PassengerRequest> getPassengers() {
		return passengers;
	}

	public void setPassengers(List<PassengerRequest> passengers) {
		this.passengers = passengers;
	}

}
