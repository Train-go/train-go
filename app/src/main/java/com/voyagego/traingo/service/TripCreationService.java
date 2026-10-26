package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.LegRuleException;
import com.voyagego.traingo.common.exception.TrainNotFoundException;
import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.model.request.LegRequest;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * Adds trips to the end of a train's schedule, the leg-chaining rules of
 * docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md. The server checks every
 * rule again, whatever the page already did: a train that has trips can only
 * continue from where its last trip ends, a leg leaves at least the turnaround
 * after the previous one arrives, and every leg needs a route between its two
 * stations. A batch is all or nothing.
 */
@Service
public class TripCreationService {

	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private final TrainRepository trainRepository;
	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;
	private final RouteRepository routeRepository;
	private final StationRepository stationRepository;
	private final TrainScheduleService trainScheduleService;

	public TripCreationService(TrainRepository trainRepository, TripRepository tripRepository,
			TripSeatRepository tripSeatRepository, RouteRepository routeRepository,
			StationRepository stationRepository, TrainScheduleService trainScheduleService) {
		this.trainRepository = trainRepository;
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
		this.routeRepository = routeRepository;
		this.stationRepository = stationRepository;
		this.trainScheduleService = trainScheduleService;
	}

	/**
	 * Creates one trip per leg, each with every seat of the fixed coach layout
	 * open for sale. Throws LegRuleException naming the leg that breaks a rule,
	 * in which case nothing is saved.
	 */
	@Transactional
	public List<Trip> createLegs(long trainId, List<LegRequest> legs, LocalDateTime now) {
		// The row lock serialises admins adding to the same train.
		Train train = trainRepository.findByIdForUpdate(trainId).orElseThrow(TrainNotFoundException::new);

		Optional<Trip> end = trainScheduleService.chainEnd(trainId);
		long originId;
		LocalDateTime floor;
		LocalDateTime lastArrival = null;
		if (end.isPresent()) {
			Station standingAt = end.get().getRoute().getDestinationStation();
			originId = standingAt.getId();
			// What the page showed must still be true: another admin may have added
			// trips since, and the legs would then start somewhere else.
			Long shown = legs.get(0).originStationId();
			if (shown != null && shown.longValue() != originId) {
				throw new LegRuleException(0, "originStationId",
						"Tàu đã ở " + standingAt.getName() + ". Tải lại trang rồi thử lại.");
			}
			lastArrival = end.get().getArrivalAt();
			floor = lastArrival.plus(TrainScheduleService.TURNAROUND);
		} else {
			if (legs.get(0).originStationId() == null) {
				throw new LegRuleException(0, "originStationId", "Chọn ga xuất phát.");
			}
			originId = legs.get(0).originStationId();
			floor = null;
		}
		// A departure must lie in the future, so a train whose schedule ended in
		// the past leaves no earlier than now.
		LocalDateTime nowToMinute = now.truncatedTo(ChronoUnit.MINUTES);
		if (floor == null || !floor.isAfter(nowToMinute)) {
			floor = nowToMinute;
			lastArrival = null;
		}

		List<Trip> created = new ArrayList<>();
		for (int i = 0; i < legs.size(); i++) {
			LegRequest leg = legs.get(i);
			LocalDateTime departure = leg.departureAt().truncatedTo(ChronoUnit.MINUTES);
			LocalDateTime arrival = leg.arrivalAt().truncatedTo(ChronoUnit.MINUTES);
			Route route = findRoute(i, originId, leg.destinationStationId());
			if (departure.isBefore(floor)) {
				throw new LegRuleException(i, "departureAt", earliestMessage(floor, route, lastArrival));
			}
			if (!arrival.isAfter(departure)) {
				throw new LegRuleException(i, "arrivalAt", "Giờ đến phải sau giờ đi.");
			}
			if (leg.basePrice() % 1000 != 0) {
				throw new LegRuleException(i, "basePrice", "Giá làm tròn tới nghìn đồng.");
			}

			Trip trip = tripRepository.save(
					new Trip(train, route, departure, arrival, leg.basePrice()));
			tripSeatRepository.saveAll(CoachLayout.seats().stream().map(seat -> new TripSeat(trip, seat)).toList());
			created.add(trip);

			originId = leg.destinationStationId();
			lastArrival = arrival;
			floor = lastArrival.plus(TrainScheduleService.TURNAROUND);
		}
		return created;
	}

	private Route findRoute(int index, long originId, long destinationId) {
		Station origin = stationRepository.findById(originId).orElseThrow(
				() -> new LegRuleException(index, index == 0 ? "originStationId" : "destinationStationId",
						"Ga xuất phát không tồn tại."));
		Station destination = stationRepository.findById(destinationId)
				.orElseThrow(() -> new LegRuleException(index, "destinationStationId", "Ga đến không tồn tại."));
		return routeRepository.findByOriginStationIdAndDestinationStationId(originId, destinationId)
				.orElseThrow(() -> new LegRuleException(index, "destinationStationId",
						"Chưa có tuyến " + origin.getName() + " → " + destination.getName() + "."));
	}

	/** Why the departure is too early: the turnaround after the last arrival, or simply the present. */
	private static String earliestMessage(LocalDateTime floor, Route route, LocalDateTime lastArrival) {
		if (lastArrival == null) {
			return "Giờ khởi hành phải từ " + dayTime(floor) + " trở đi.";
		}
		return "Sớm nhất " + dayTime(floor) + ": tàu đến " + route.getOriginStation().getName() + " lúc "
				+ lastArrival.format(TIME) + " và cần " + TrainScheduleService.TURNAROUND.toMinutes()
				+ " phút quay đầu.";
	}

	private static String dayTime(LocalDateTime time) {
		return SHORT_WEEKDAYS[time.getDayOfWeek().getValue() - 1] + " " + time.format(DAY_TIME);
	}

}
