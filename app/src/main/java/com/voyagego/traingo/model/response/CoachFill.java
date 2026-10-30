package com.voyagego.traingo.model.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.dto.TripCoachCount;

/** How full one coach of a trip is, from its real seats. */
public record CoachFill(int number, int capacity, long sold, int percent) {

	/** Every coach of each trip that has a booked seat; a trip with none has no entry. */
	public static Map<Long, List<CoachFill>> byTrip(List<TripCoachCount> counts) {
		Map<Long, Map<Integer, Long>> sold = new HashMap<>();
		counts.forEach(count -> sold.computeIfAbsent(count.getTripId(), id -> new HashMap<>())
				.put(count.getCoachNumber(), count.getTotal()));
		Map<Long, List<CoachFill>> fills = new HashMap<>();
		sold.forEach((tripId, perCoach) -> fills.put(tripId, of(perCoach)));
		return fills;
	}

	/** Every coach of the fixed layout, empty ones included. */
	public static List<CoachFill> of(Map<Integer, Long> soldPerCoach) {
		return CoachLayout.coaches().stream().map(coach -> {
			long sold = soldPerCoach.getOrDefault(coach.number(), 0L);
			return new CoachFill(coach.number(), coach.seatCount(), sold,
					(int) Math.round(sold * 100.0 / coach.seatCount()));
		}).toList();
	}

}
