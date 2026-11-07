package com.voyagego.traingo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.entity.Route;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.model.dto.CoachLayout;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * Keeps demo data from running out (docs/decisions/0017-bo-nap-chuyen-demo.md).
 * Every start it tops up each demo train with a daily round trip for the next
 * {@value #DAYS_AHEAD} days, skipping departures that already exist or already
 * left. Trips are saved with the same fixed seat layout an admin gets, but not
 * through TripCreationService: that service chains legs from the train's last
 * station, while this seeder places each day on its own.
 *
 * Turn it off with {@code traingo.demo-trips.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "traingo.demo-trips.enabled", havingValue = "true", matchIfMissing = true)
public class DemoTripSeeder implements ApplicationRunner {

	static final int DAYS_AHEAD = 14;

	private static final Logger log = LoggerFactory.getLogger(DemoTripSeeder.class);

	/** One train going between two stations and back every day, at the given hours. */
	private record Rotation(String trainCode, String stationA, String stationB, int hourOut, int hourBack) {
	}

	private static final List<Rotation> ROTATIONS = List.of(
			new Rotation("SE1", "HNI", "THA", 6, 14),
			new Rotation("SE2", "HUE", "DNG", 8, 15),
			new Rotation("SE3", "NTR", "SGN", 7, 16));

	private final TrainRepository trainRepository;
	private final StationRepository stationRepository;
	private final RouteRepository routeRepository;
	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;

	public DemoTripSeeder(TrainRepository trainRepository, StationRepository stationRepository,
			RouteRepository routeRepository, TripRepository tripRepository, TripSeatRepository tripSeatRepository) {
		this.trainRepository = trainRepository;
		this.stationRepository = stationRepository;
		this.routeRepository = routeRepository;
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int created = seed(LocalDateTime.now());
		if (created > 0) {
			log.info("Demo trips: added {} trips for the next {} days", created, DAYS_AHEAD);
		}
	}

	/** Returns how many trips were added. */
	int seed(LocalDateTime now) {
		LocalDate today = now.toLocalDate();
		int created = 0;
		for (Rotation rotation : ROTATIONS) {
			Optional<Train> train = trainRepository.findByCodeAndDeletedFalse(rotation.trainCode());
			Optional<Route> out = route(rotation.stationA(), rotation.stationB());
			Optional<Route> back = route(rotation.stationB(), rotation.stationA());
			if (train.isEmpty() || out.isEmpty() || back.isEmpty()) {
				log.debug("Demo trips: skipped {}, the train or a route is missing", rotation.trainCode());
				continue;
			}
			for (int day = 0; day < DAYS_AHEAD; day++) {
				LocalDate date = today.plusDays(day);
				created += add(train.get(), out.get(), date.atTime(rotation.hourOut(), 0), now);
				created += add(train.get(), back.get(), date.atTime(rotation.hourBack(), 0), now);
			}
		}
		return created;
	}

	private Optional<Route> route(String originCode, String destinationCode) {
		return stationRepository.findByCode(originCode).flatMap(origin -> stationRepository.findByCode(destinationCode)
				.flatMap(destination -> routeRepository.findByOriginStationIdAndDestinationStationId(origin.getId(),
						destination.getId())));
	}

	private int add(Train train, Route route, LocalDateTime departure, LocalDateTime now) {
		if (!departure.isAfter(now)
				|| tripRepository.existsByTrainIdAndRouteIdAndDepartureAt(train.getId(), route.getId(), departure)) {
			return 0;
		}
		// Running time is 1 minute per km rounded to 5 minutes; the price is about 1.000 dong per km rounded to 10.000.
		int minutes = Math.round(route.getDistanceKm() / 5f) * 5;
		long price = Math.round(route.getDistanceKm() / 10.0) * 10_000L;
		Trip trip = tripRepository.save(new Trip(train, route, departure, departure.plusMinutes(minutes), price));
		tripSeatRepository.saveAll(CoachLayout.seats().stream().map(seat -> new TripSeat(trip, seat)).toList());
		return 1;
	}

}
