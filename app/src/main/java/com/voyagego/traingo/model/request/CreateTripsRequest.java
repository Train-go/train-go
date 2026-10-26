package com.voyagego.traingo.model.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Legs to add after a train's last trip, in travel order, all or none. A
 * failed leg is reported under {@code legs[i].field}, so a page can show the
 * message next to the right row.
 */
public record CreateTripsRequest(
		@NotEmpty(message = "Chọn ít nhất một chặng.") @Size(max = 14, message = "Thêm tối đa 14 chặng một lần.")
		List<@NotNull(message = "Chặng không hợp lệ.") @Valid LegRequest> legs) {
}
