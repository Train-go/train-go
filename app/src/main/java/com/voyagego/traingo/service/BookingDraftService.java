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
import com.voyagego.traingo.common.TextUtils;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.entity.TripSeatStatus;
import com.voyagego.traingo.model.dto.BookingDraft;
import com.voyagego.traingo.model.dto.BookingDraft.PassengerDraft;
import com.voyagego.traingo.model.request.PassengerRequest;
import com.voyagego.traingo.model.request.PassengersForm;
import com.voyagego.traingo.model.response.PassengerPage;
import com.voyagego.traingo.model.response.PassengerPage.Slot;
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
	 * The passenger page for a draft: the trip, a slot per chosen seat in the
	 * order they were chosen, and the account holder's name and masked phone.
	 *
	 * @throws com.voyagego.traingo.common.exception.TripNotBookableException the trip can no longer be booked
	 */
	@Transactional(readOnly = true)
	public PassengerPage passengerPage(BookingDraft draft, TrainGoUserDetails user, LocalDateTime now) {
		var trip = seatMapService.seatMap(draft.getTripId(), now).trip();
		Map<Long, TripSeat> seats = new HashMap<>();
		tripSeatRepository.findAllById(draft.getTripSeatIds()).forEach(seat -> seats.put(seat.getId(), seat));
		List<Slot> slots = new ArrayList<>();
		for (Long id : draft.getTripSeatIds()) {
			TripSeat seat = seats.get(id);
			slots.add(new Slot(slots.size() + 1, id, seat.getCoachNumber(), seat.getSeatCode(),
					SeatMapService.label(seat.getCoachType()), trip.price()));
		}
		return new PassengerPage(trip, slots, trip.price() * slots.size(), user.getFullName(),
				TextUtils.maskPhone(user.getPhoneNumber()));
	}

	/** The form as the draft has it, to fill the page the first time it opens. */
	public PassengersForm formOf(BookingDraft draft) {
		PassengersForm form = new PassengersForm();
		for (PassengerDraft passenger : draft.getPassengers()) {
			PassengerRequest line = new PassengerRequest();
			line.setFullName(passenger.fullName());
			line.setIdentityNumber(passenger.identityNumber());
			form.getPassengers().add(line);
		}
		return form;
	}

	/**
	 * A draft with the passengers of a validated form, line i for seat i.
	 *
	 * @throws SeatSelectionException the form has a different number of lines than the draft has seats
	 */
	public BookingDraft updatePassengers(BookingDraft draft, PassengersForm form) {
		if (form.getPassengers().size() != draft.getTripSeatIds().size()) {
			throw new SeatSelectionException("Số hành khách phải bằng số chỗ đã chọn.");
		}
		List<PassengerDraft> passengers = form.getPassengers().stream()
				.map(line -> new PassengerDraft(line.getFullName(), line.getIdentityNumber()))
				.toList();
		return new BookingDraft(draft.getTripId(), draft.getTripSeatIds(), passengers);
	}

	/**
	 * Keeps what the customer typed so far when they leave to choose seats
	 * again. Nothing is checked: a blank name keeps the old one and the CCCD
	 * keeps only its digits. A form of another size is ignored.
	 */
	public BookingDraft keepEdits(BookingDraft draft, PassengersForm form) {
		if (form.getPassengers().size() != draft.getTripSeatIds().size()) {
			return draft;
		}
		List<PassengerDraft> passengers = new ArrayList<>();
		for (int i = 0; i < draft.getPassengers().size(); i++) {
			PassengerDraft old = draft.getPassengers().get(i);
			PassengerRequest line = form.getPassengers().get(i);
			String name = line.getFullName();
			String digits = line.getIdentityNumber() == null ? "" : line.getIdentityNumber().replaceAll("\\D", "");
			passengers.add(new PassengerDraft(
					name == null || name.isBlank() || name.length() > 100 ? old.fullName() : name,
					digits.length() > 12 ? digits.substring(0, 12) : digits));
		}
		return new BookingDraft(draft.getTripId(), draft.getTripSeatIds(), passengers);
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
