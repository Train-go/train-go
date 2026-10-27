package com.voyagego.traingo.model.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Several routes created at once (#38, #39): the suggested routes after a new
 * station, or the routes from one station to many. Each pair is one direction.
 * One pair that breaks a rule fails the whole request and nothing is created.
 */
public class RouteBatchRequest {

	public static final int MAX_ROUTES = 40;

	@NotEmpty(message = "Chọn ít nhất một tuyến.")
	@Size(max = MAX_ROUTES, message = "Mỗi lần tạo tối đa 40 tuyến.")
	@Valid
	private List<RoutePair> routes = new ArrayList<>();

	public List<RoutePair> getRoutes() {
		return routes;
	}

	public void setRoutes(List<RoutePair> routes) {
		this.routes = routes;
	}

	/** One direction: from the origin station to the destination station. */
	public static class RoutePair {

		@NotNull(message = "Vui lòng chọn ga đi.")
		private Long originStationId;

		@NotNull(message = "Vui lòng chọn ga đến.")
		private Long destinationStationId;

		@Positive(message = "Khoảng cách phải lớn hơn 0 km.")
		private int distanceKm;

		@AssertTrue(message = "Ga đến phải khác ga đi.")
		public boolean isDifferentStations() {
			return originStationId == null || destinationStationId == null
					|| !originStationId.equals(destinationStationId);
		}

		public Long getOriginStationId() {
			return originStationId;
		}

		public void setOriginStationId(Long originStationId) {
			this.originStationId = originStationId;
		}

		public Long getDestinationStationId() {
			return destinationStationId;
		}

		public void setDestinationStationId(Long destinationStationId) {
			this.destinationStationId = destinationStationId;
		}

		public int getDistanceKm() {
			return distanceKm;
		}

		public void setDistanceKm(int distanceKm) {
			this.distanceKm = distanceKm;
		}

	}

}
