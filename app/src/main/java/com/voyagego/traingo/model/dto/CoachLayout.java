package com.voyagego.traingo.model.dto;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import com.voyagego.traingo.entity.CoachType;

/**
 * Fixed layout shared by all trains. Admins do not manage coaches (ADR 0009).
 * Change this configuration to change future trips; existing trips retain their
 * copied seat data.
 */
public final class CoachLayout {

	public record Coach(int number, CoachType type, String seatPrefix, int seatCount) {
	}

	public record Seat(int coachNumber, CoachType type, String seatCode, Integer cabinNumber) {
	}

	private static final List<Coach> COACHES = List.of(
			new Coach(1, CoachType.SEAT, "A", 32),
			new Coach(2, CoachType.SEAT, "A", 32),
			new Coach(3, CoachType.SLEEPER, "B", 16),
			new Coach(4, CoachType.SLEEPER, "B", 16));

	private static final List<Seat> SEATS = COACHES.stream()
			.flatMap(coach -> IntStream.rangeClosed(1, coach.seatCount())
					.mapToObj(index -> new Seat(coach.number(), coach.type(),
							seatCode(coach.seatPrefix(), index), cabinOf(coach.type(), index))))
			.toList();

	private CoachLayout() {
	}

	public static List<Coach> coaches() {
		return COACHES;
	}

	public static List<Seat> seats() {
		return SEATS;
	}

	public static int totalSeats() {
		return SEATS.size();
	}

	/** Indexes are one-based within a coach. Seat coaches have no cabins. */
	public static Integer cabinOf(CoachType type, int index) {
		return type == CoachType.SLEEPER ? (index - 1) / 4 + 1 : null;
	}

	public static String seatCode(String prefix, int index) {
		return String.format(Locale.ROOT, "%s%02d", prefix, index);
	}
}
