package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.TripNotBookableException;
import com.voyagego.traingo.entity.CoachType;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.entity.TripSeatStatus;
import com.voyagego.traingo.entity.TripStatus;
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.model.response.SeatMap;
import com.voyagego.traingo.model.response.SeatMap.CoachMap;
import com.voyagego.traingo.model.response.SeatMap.SeatCell;
import com.voyagego.traingo.model.response.SeatMap.TripSummary;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * The seat page's view of one trip (issue #30). It reads seats as they are
 * now and locks nothing: a seat is only taken at payment (ADR 0010).
 */
@Service
public class SeatMapService {

	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;

	public SeatMapService(TripRepository tripRepository, TripSeatRepository tripSeatRepository) {
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
	}

	@Transactional(readOnly = true)
	public SeatMap seatMap(long tripId, LocalDateTime now) {
		Trip trip = requireBookable(tripId, now);
		Map<Integer, List<TripSeat>> byCoach = new TreeMap<>();
		for (TripSeat seat : tripSeatRepository.findByTripIdOrderByCoachNumberAscSeatCodeAsc(tripId)) {
			byCoach.computeIfAbsent(seat.getCoachNumber(), number -> new ArrayList<>()).add(seat);
		}
		List<CoachMap> coaches = byCoach.entrySet().stream().map(entry -> coach(entry.getKey(), entry.getValue()))
				.toList();
		return new SeatMap(summary(trip), coaches);
	}

	/**
	 * The trip when it can still be booked: scheduled and not yet departed,
	 * the same "chưa khởi hành" as the rest of the booking flow (D8).
	 *
	 * @throws TripNotBookableException with the reason, written for the customer
	 */
	@Transactional(readOnly = true)
	public Trip requireBookable(long tripId, LocalDateTime now) {
		Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotBookableException::missing);
		if (trip.getStatus() == TripStatus.CANCELLED) {
			throw TripNotBookableException.cancelled();
		}
		if (trip.hasDeparted(now)) {
			throw TripNotBookableException.departed();
		}
		return trip;
	}

	private static TripSummary summary(Trip trip) {
		LocalDateTime departure = trip.getDepartureAt();
		LocalDateTime arrival = trip.getArrivalAt();
		int nextDay = (int) ChronoUnit.DAYS.between(departure.toLocalDate(), arrival.toLocalDate());
		Station origin = trip.getRoute().getOriginStation();
		Station destination = trip.getRoute().getDestinationStation();
		return new TripSummary(trip.getId(), trip.getTrain().getCode(), origin.getCode(),
				PopularRoute.placeName(origin.getName()), destination.getCode(),
				PopularRoute.placeName(destination.getName()), departure, arrival,
				TripSearchService.durationLabel(Duration.between(departure, arrival)), nextDay, trip.getBasePrice());
	}

	private static CoachMap coach(int number, List<TripSeat> seats) {
		CoachType type = seats.get(0).getCoachType();
		List<SeatCell> cells = seats.stream()
				.map(seat -> new SeatCell(seat.getId(), seat.getSeatCode(), seat.getCabinNumber(),
						seat.getStatus() == TripSeatStatus.AVAILABLE))
				.toList();
		int available = (int) cells.stream().filter(SeatCell::available).count();
		return new CoachMap(number, type, label(type), available, cells.size(), cells);
	}

	public static String label(CoachType type) {
		return type == CoachType.SEAT ? "Ghế ngồi" : "Giường nằm";
	}

}
