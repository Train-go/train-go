package com.voyagego.traingo.admin.model;

public class Trip {

	private final String trainCode;
	private final String route;
	private final String departureDate;
	private final String departureTime;
	private final String arrivalTime;
	private final long basePrice;
	private final String status;

	public Trip(String trainCode, String route, String departureDate, String departureTime, String arrivalTime,
			long basePrice, String status) {
		this.trainCode = trainCode;
		this.route = route;
		this.departureDate = departureDate;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.basePrice = basePrice;
		this.status = status;
	}

	public String getTrainCode() {
		return trainCode;
	}

	public String getRoute() {
		return route;
	}

	public String getDepartureDate() {
		return departureDate;
	}

	public String getDepartureTime() {
		return departureTime;
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public long getBasePrice() {
		return basePrice;
	}

	public String getStatus() {
		return status;
	}

}
