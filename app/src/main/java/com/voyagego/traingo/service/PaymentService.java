package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.SeatSelectionException;
import com.voyagego.traingo.common.exception.SeatTakenException;
import com.voyagego.traingo.entity.Booking;
import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.entity.TripSeatStatus;
import com.voyagego.traingo.model.dto.BookingDraft;
import com.voyagego.traingo.model.dto.BookingDraft.PassengerDraft;
import com.voyagego.traingo.model.response.Invoice;
import com.voyagego.traingo.model.response.Invoice.Line;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;
import com.voyagego.traingo.repository.TripRepository;
import com.voyagego.traingo.repository.TripSeatRepository;
import com.voyagego.traingo.repository.UserRepository;

/**
 * The mock payment: no bank, no card, no callback. One transaction either does
 * all of the following or nothing (docs/product/booking-flow.md):
 *
 * <ol>
 * <li>checks the trip is still SCHEDULED and has not left;
 * <li>turns the chosen seats AVAILABLE to BOOKED with one conditional UPDATE;
 * fewer rows changed than seats chosen means someone else got one first;
 * <li>creates the booking, CONFIRMED and PAID;
 * <li>creates one ticket per passenger at the trip's price.
 * </ol>
 *
 * Two states must never exist: a CONFIRMED booking whose seat is still
 * AVAILABLE, and a BOOKED seat that belongs to no confirmed booking. Any
 * exception thrown from {@link #pay} rolls the whole transaction back, which is
 * what undoes the seats already booked when one of them was taken.
 *
 * Repeating a payment after a failure is PaymentFacade's job, not this class's,
 * because a repeat needs a new transaction.
 */
@Service
public class PaymentService {

	private final SeatMapService seatMapService;
	private final TripRepository tripRepository;
	private final TripSeatRepository tripSeatRepository;
	private final BookingRepository bookingRepository;
	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final BookingCodeGenerator codeGenerator;

	public PaymentService(SeatMapService seatMapService, TripRepository tripRepository,
			TripSeatRepository tripSeatRepository, BookingRepository bookingRepository,
			TicketRepository ticketRepository, UserRepository userRepository, BookingCodeGenerator codeGenerator) {
		this.seatMapService = seatMapService;
		this.tripRepository = tripRepository;
		this.tripSeatRepository = tripSeatRepository;
		this.bookingRepository = bookingRepository;
		this.ticketRepository = ticketRepository;
		this.userRepository = userRepository;
		this.codeGenerator = codeGenerator;
	}

	/**
	 * The bill for a draft, for the payment page. Writes nothing.
	 *
	 * @throws com.voyagego.traingo.common.exception.TripNotBookableException the trip can no longer be booked
	 * @throws SeatSelectionException the draft is not ready to pay (see {@link #requireComplete})
	 */
	@Transactional(readOnly = true)
	public Invoice invoice(BookingDraft draft, LocalDateTime now) {
		requireComplete(draft);
		var trip = seatMapService.seatMap(draft.getTripId(), now).trip();
		Map<Long, TripSeat> seats = seatsById(draft);
		List<Line> lines = new ArrayList<>();
		for (int i = 0; i < draft.getTripSeatIds().size(); i++) {
			TripSeat seat = seats.get(draft.getTripSeatIds().get(i));
			lines.add(new Line(seat.getSeatCode(), seat.getCoachNumber(), SeatMapService.label(seat.getCoachType()),
					draft.getPassengers().get(i).fullName(), trip.price()));
		}
		return new Invoice(trip, lines, trip.price() * lines.size());
	}

	/**
	 * Pays a draft for the customer.
	 *
	 * @return the new booking, CONFIRMED and PAID
	 * @throws com.voyagego.traingo.common.exception.TripNotBookableException the trip was cancelled or has left
	 * @throws SeatSelectionException the draft is not ready to pay
	 * @throws SeatTakenException some chosen seats were booked by someone else; nothing was created
	 */
	@Transactional
	public Booking pay(BookingDraft draft, long customerId, LocalDateTime now) {
		requireComplete(draft);
		Trip trip = seatMapService.requireBookable(draft.getTripId(), now);
		long price = trip.getBasePrice();
		List<Long> seatIds = draft.getTripSeatIds();

		// Lock the seats first, so the ones already taken can be named before this
		// transaction changes any; reading them after the UPDATE would also show the
		// seats this very UPDATE just booked.
		List<TripSeat> locked = tripSeatRepository.findByIdInForUpdate(seatIds);
		if (locked.size() != seatIds.size()
				|| locked.stream().anyMatch(seat -> !seat.getTrip().getId().equals(trip.getId()))) {
			throw new SeatSelectionException("Có chỗ không thuộc chuyến này.");
		}
		List<TripSeat> taken = locked.stream().filter(seat -> seat.getStatus() != TripSeatStatus.AVAILABLE)
				.sorted(Comparator.comparingInt(seat -> seatIds.indexOf(seat.getId()))).toList();
		if (!taken.isEmpty()) {
			throw SeatTakenException.of(taken);
		}
		// The conditional UPDATE stays the guard: it only changes a seat that is still AVAILABLE.
		if (tripSeatRepository.bookIfAvailable(trip.getId(), seatIds) != seatIds.size()) {
			throw new IllegalStateException("Locked seats of trip " + trip.getId() + " were not all booked");
		}

		// The UPDATE cleared the persistence context, so everything is read again from here.
		Map<Long, TripSeat> seats = seatsById(draft);
		Trip bookedTrip = tripRepository.findById(trip.getId()).orElseThrow();
		String code = codeGenerator.nextBookingCode(YearMonth.from(now));
		Booking booking = bookingRepository.saveAndFlush(Booking.paid(code, userRepository.getReferenceById(customerId),
				bookedTrip, price * seatIds.size(), now));

		List<Ticket> tickets = new ArrayList<>();
		for (int i = 0; i < seatIds.size(); i++) {
			PassengerDraft passenger = draft.getPassengers().get(i);
			tickets.add(new Ticket(booking, seats.get(seatIds.get(i)), passenger.fullName(),
					passenger.identityNumber(), price, BookingCodeGenerator.ticketCode(code, i + 1), now));
		}
		ticketRepository.saveAll(tickets);
		return booking;
	}

	/**
	 * A draft can only be paid with 1 to 10 seats and one passenger per seat,
	 * each with a name of up to 100 characters and a 12-digit CCCD. The passenger
	 * page checks the same, but a draft reaches payment from the session, so the
	 * rule is repeated here (D6, D10).
	 */
	void requireComplete(BookingDraft draft) {
		int seats = draft.getTripSeatIds().size();
		if (seats < 1 || seats > BookingDraftService.MAX_SEATS) {
			throw new SeatSelectionException("Mỗi lần đặt từ 1 đến " + BookingDraftService.MAX_SEATS + " chỗ.");
		}
		if (draft.getPassengers().size() != seats) {
			throw new SeatSelectionException("Số hành khách phải bằng số chỗ đã chọn.");
		}
		for (PassengerDraft passenger : draft.getPassengers()) {
			String name = passenger.fullName();
			String identity = passenger.identityNumber();
			if (name == null || name.isBlank() || name.length() > 100 || identity == null
					|| !identity.matches("\\d{12}")) {
				throw new SeatSelectionException("Hãy nhập đủ họ tên và số CCCD cho từng vé.");
			}
		}
	}

	private Map<Long, TripSeat> seatsById(BookingDraft draft) {
		Map<Long, TripSeat> seats = new HashMap<>();
		tripSeatRepository.findAllById(draft.getTripSeatIds()).forEach(seat -> seats.put(seat.getId(), seat));
		return seats;
	}

}
