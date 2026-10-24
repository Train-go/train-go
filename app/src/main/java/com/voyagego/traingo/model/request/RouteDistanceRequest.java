package com.voyagego.traingo.model.request;

import jakarta.validation.constraints.Positive;

public class RouteDistanceRequest {
	@Positive(message = "Khoảng cách phải lớn hơn 0 km.")
	private int distanceKm;

	public int getDistanceKm() { return distanceKm; }
	public void setDistanceKm(int value) { distanceKm = value; }
}
