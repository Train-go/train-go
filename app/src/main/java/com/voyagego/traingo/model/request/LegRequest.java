package com.voyagego.traingo.model.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One leg of a batch of trips to add to a train. The origin is the station
 * where the train stands, so only the first leg of a train with no trip yet
 * names it; every other leg starts where the one before it ends.
 */
public record LegRequest(Long originStationId,
		@NotNull(message = "Chọn ga đến.") Long destinationStationId,
		@NotNull(message = "Nhập giờ đi.") LocalDateTime departureAt,
		@NotNull(message = "Nhập giờ đến.") LocalDateTime arrivalAt,
		@NotNull(message = "Nhập giá cơ bản.") @PositiveOrZero(message = "Giá không được âm.") Long basePrice) {
}
