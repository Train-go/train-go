package com.voyagego.traingo.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RouteCreateRequest {
	@NotNull(message = "Vui lòng chọn ga đi.")
	private Long originStationId;
	@NotNull(message = "Vui lòng chọn ga đến.")
	private Long destinationStationId;
	@Positive(message = "Khoảng cách phải lớn hơn 0 km.")
	private int distanceKm;
	private boolean bothWays;

	// The form/API adapter must map differentStations to destinationStationId.
	@AssertTrue(message = "Ga đến phải khác ga đi.")
	public boolean isDifferentStations() {
		return originStationId == null || destinationStationId == null
				|| !originStationId.equals(destinationStationId);
	}

	public Long getOriginStationId() { return originStationId; }
	public void setOriginStationId(Long value) { originStationId = value; }
	public Long getDestinationStationId() { return destinationStationId; }
	public void setDestinationStationId(Long value) { destinationStationId = value; }
	public int getDistanceKm() { return distanceKm; }
	public void setDistanceKm(int value) { distanceKm = value; }
	public boolean isBothWays() { return bothWays; }
	public void setBothWays(boolean value) { bothWays = value; }
}
