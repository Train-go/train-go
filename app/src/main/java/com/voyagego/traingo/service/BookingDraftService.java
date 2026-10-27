package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.SeatSelectionException;
import com.voyagego.traingo.common.exception.SeatTakenException;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.entity.TripSeatStatus;
import com.voyagego.traingo.model.dto.BookingDraft;
import com.voyagego.traingo.model.dto.BookingDraft.PassengerDraft;
import com.voyagego.traingo.repository.TripSeatRepository;

/**
 * Turns the seats picked on the seat page into a booking draft (issue #30).
 * It writes nothing to the database: the seats are re-read so a seat sold
 * meanwhile is reported, but they stay free until payment (ADR 0010).
 */
@Service
public class BookingDraftService {

	/** Tickets per booking (D6). */
	public static final int MAX_SEATS = 10;

	private final SeatMapService seatMapService;
	private final TripSeatRepository tripSeatRepository;

	public BookingDraftService(SeatMapService seatMapService, TripSeatRepository tripSeatRepository) {
		this.seatMapService = seatMapService;
		this.tripSeatRepository = tripSeatRepository;
	}

	/**
	 * @param previous the draft now in the session, if any; a passenger name the
	 *                 customer already edited is kept when the same seat of the
	 *                 same trip is chosen again
	 * @throws com.voyagego.traingo.common.exception.TripNotBookableException the trip cannot be booked
	 * @throws SeatSelectionException the list is empty, over 10, repeats a seat or holds a seat of another trip
	 * @throws SeatTakenException some seats are no longer free
	 */
	@Transactional(readOnly = true)
	public BookingDraft chooseSeats(long tripId, List<Long> seatIds, TrainGoUserDetails user,
			BookingDraft previous, LocalDateTime now) {
		seatMapService.requireBookable(tripId, now);
		if (seatIds == null || seatIds.isEmpty()) {
			throw new SeatSelectionException("Chọn ít nhất 1 chỗ.");
		}
		if (seatIds.size() > MAX_SEATS) {
			throw new SeatSelectionException("Mỗi lần đặt tối đa " + MAX_SEATS + " chỗ.");
		}
		if (new HashSet<>(seatIds).size() != seatIds.size()) {
			throw new SeatSelectionException("Một chỗ được chọn hai lần.");
		}

		Map<Long, TripSeat> seats = new HashMap<>();
		tripSeatRepository.findAllById(seatIds).forEach(seat -> seats.put(seat.getId(), seat));
		for (Long id : seatIds) {
			TripSeat seat = seats.get(id);
			if (seat == null || !seat.getTrip().getId().equals(tripId)) {
				throw new SeatSelectionException("Có chỗ không thuộc chuyến này.");
			}
		}
		rejectTaken(seatIds, seats);

		Map<Long, PassengerDraft> kept = new HashMap<>();
		if (previous != null && previous.getTripId().equals(tripId)) {
			for (int i = 0; i < previous.getTripSeatIds().size(); i++) {
				kept.put(previous.getTripSeatIds().get(i), previous.getPassengers().get(i));
			}
		}
		List<PassengerDraft> passengers = seatIds.stream()
				.map(id -> kept.getOrDefault(id, new PassengerDraft(user.getFullName(), "")))
				.toList();
		return new BookingDraft(tripId, seatIds, passengers);
	}

	/**
	 * A seat code repeats across coaches (A05 is in coach 1 and 2), so a code
	 * that would be ambiguous in the sentence carries its coach.
	 */
	private static void rejectTaken(List<Long> seatIds, Map<Long, TripSeat> seats) {
		List<TripSeat> taken = seatIds.stream().map(seats::get)
				.filter(seat -> seat.getStatus() != TripSeatStatus.AVAILABLE).toList();
		if (taken.isEmpty()) {
			return;
		}
		List<String> labels = new ArrayList<>();
		for (TripSeat seat : taken) {
			boolean repeated = taken.stream().filter(other -> other.getSeatCode().equals(seat.getSeatCode())).count() > 1;
			labels.add(repeated ? seat.getSeatCode() + " (toa " + seat.getCoachNumber() + ")" : seat.getSeatCode());
		}
		throw new SeatTakenException(labels, taken.stream().map(TripSeat::getId).toList());
	}

}
