package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.TextUtils;
import com.voyagego.traingo.common.exception.TripNotFoundException;
import com.voyagego.traingo.entity.CoachType;
import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.entity.TripSeatStatus;
import com.voyagego.traingo.model.response.TripDetail;
import com.voyagego.traingo.model.response.TripDetail.CoachView;
import com.voyagego.traingo.model.response.TripDetail.PassengerRow;
import com.voyagego.traingo.model.response.TripDetail.SeatView;
import com.voyagego.traingo.repository.TicketRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * The admin's view of one trip: its facts, every seat of its coaches with the
 * passenger sitting there, and the passenger list. Only seats of confirmed
 * bookings are sold; a cancelled booking has already released its seats.
 * Every method takes {@code now} from its caller, so one page shows one moment.
 */
@Service
@Transactional(readOnly = true)
public class TripAdminService {

	private static final String[] SHORT_WEEKDAYS = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;
	private final TicketRepository ticketRepository;

	public TripAdminService(TripRepository tripRepository, TripSeatRepository tripSeatRepository,
			TicketRepository ticketRepository) {
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
		this.ticketRepository = ticketRepository;
	}

	/** Throws TripNotFoundException, shown as a 404, for an unknown id. */
	public TripDetail detail(long tripId, LocalDateTime now) {
		Trip trip = tripRepository.findDetailById(tripId).orElseThrow(TripNotFoundException::new);
		List<TripSeat> seats = tripSeatRepository.findByTripIdOrderByCoachNumberAscSeatCodeAsc(tripId);
		List<Ticket> tickets = ticketRepository.findConfirmedByTrip(tripId);
		Map<Long, Ticket> ticketBySeat = tickets.stream()
				.collect(Collectors.toMap(ticket -> ticket.getTripSeat().getId(), ticket -> ticket, (first, second) -> first));

		// The seats arrive ordered by coach, so a coach is one run of equal numbers.
		Map<Integer, List<TripSeat>> byCoach = new LinkedHashMap<>();
		for (TripSeat seat : seats) {
			byCoach.computeIfAbsent(seat.getCoachNumber(), number -> new ArrayList<>()).add(seat);
		}
		List<CoachView> coaches = new ArrayList<>();
		int booked = 0;
		for (Map.Entry<Integer, List<TripSeat>> coach : byCoach.entrySet()) {
			List<SeatView> views = coach.getValue().stream().map(seat -> toSeat(seat, ticketBySeat.get(seat.getId())))
					.toList();
			int sold = (int) views.stream().filter(SeatView::sold).count();
			booked += sold;
			boolean sleeper = coach.getValue().get(0).getCoachType() == CoachType.SLEEPER;
			coaches.add(new CoachView(coach.getKey(), sleeper, sleeper ? "Giường nằm" : "Ghế ngồi", views.size(), sold,
					views));
		}

		List<PassengerRow> passengers = tickets.stream()
				.map(ticket -> new PassengerRow(ticket.getFullName(), TextUtils.maskId(ticket.getIdentityNumber()),
						ticket.getTripSeat().getCoachNumber(), ticket.getTripSeat().getSeatCode(),
						ticket.getBooking().getBookingCode(), ticket.getTicketPrice()))
				.toList();

		return new TripDetail(trip.getId(), trip.getTrain().getId(), trip.getTrain().getCode(),
				trip.getTrain().getName(), trip.getRoute().getOriginStation().getName() + " → "
						+ trip.getRoute().getDestinationStation().getName(),
				trip.phaseLabel(now), phaseCss(trip, now), dayTime(trip.getDepartureAt()), arrivalLabel(trip),
				trip.getRoute().getDistanceKm(), trip.getBasePrice(), booked, seats.size(), coaches, passengers);
	}

	private static SeatView toSeat(TripSeat seat, Ticket ticket) {
		boolean sold = seat.getStatus() == TripSeatStatus.BOOKED;
		return new SeatView(seat.getSeatCode(), seat.getCabinNumber(), sold, ticket == null ? null : ticket.getFullName(),
				ticket == null ? null : ticket.getBooking().getBookingCode());
	}

	/** The .tag modifiers in css/admin/admin.css. */
	private static String phaseCss(Trip trip, LocalDateTime now) {
		if (trip.isCancelled()) {
			return "cancelled";
		}
		if (!trip.hasDeparted(now)) {
			return "scheduled";
		}
		return trip.isRunning(now) ? "running" : "done";
	}

	/** "T5 22/10 08:00". */
	private static String dayTime(LocalDateTime time) {
		return SHORT_WEEKDAYS[time.getDayOfWeek().getValue() - 1] + " " + time.format(DAY_MONTH) + " "
				+ time.format(TIME);
	}

	/** The arrival time, with its day when the trip arrives on a later day than it departs. */
	private static String arrivalLabel(Trip trip) {
		long days = ChronoUnit.DAYS.between(trip.getDepartureAt().toLocalDate(), trip.getArrivalAt().toLocalDate());
		return days == 0 ? trip.getArrivalAt().format(TIME) : dayTime(trip.getArrivalAt());
	}

}
