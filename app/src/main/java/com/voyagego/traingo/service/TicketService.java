package com.voyagego.traingo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.TextUtils;
import com.voyagego.traingo.common.exception.TicketNotFoundException;
import com.voyagego.traingo.entity.BookingStatus;
import com.voyagego.traingo.entity.Ticket;
import com.voyagego.traingo.entity.Trip;
import com.voyagego.traingo.entity.TripSeat;
import com.voyagego.traingo.model.response.PopularRoute;
import com.voyagego.traingo.model.response.TicketView;
import com.voyagego.traingo.repository.BookingRepository;
import com.voyagego.traingo.repository.TicketRepository;

/**
 * The e-ticket pages (issue #34). Only the owner of the booking reads its
 * tickets: a ticket or booking of someone else is TicketNotFoundException, the
 * same answer as for one that does not exist (docs/product/auth.md). A ticket
 * of a cancelled booking is still shown, marked void.
 */
@Service
public class TicketService {

	private final TicketRepository ticketRepository;
	private final BookingRepository bookingRepository;

	public TicketService(TicketRepository ticketRepository, BookingRepository bookingRepository) {
		this.ticketRepository = ticketRepository;
		this.bookingRepository = bookingRepository;
	}

	@Transactional(readOnly = true)
	public TicketView ticket(long customerId, long ticketId) {
		Ticket ticket = ticketRepository.findDetailById(ticketId)
				.filter(found -> found.getBooking().getCustomer().getId().equals(customerId))
				.orElseThrow(TicketNotFoundException::new);
		return view(ticket);
	}

	/** Every ticket of the booking, -01 first. */
	@Transactional(readOnly = true)
	public List<TicketView> ticketsOfBooking(long customerId, long bookingId) {
		if (!bookingRepository.existsByIdAndCustomerId(bookingId, customerId)) {
			throw new TicketNotFoundException();
		}
		return ticketRepository.findDetailsByBookingId(bookingId).stream().map(TicketService::view).toList();
	}

	private static TicketView view(Ticket ticket) {
		TripSeat seat = ticket.getTripSeat();
		Trip trip = seat.getTrip();
		LocalDateTime departure = trip.getDepartureAt();
		LocalDateTime arrival = trip.getArrivalAt();
		return new TicketView(ticket.getId(), ticket.getBooking().getId(), ticket.getTicketCode(),
				ticket.getBooking().getBookingCode(), ticket.getFullName(),
				TextUtils.maskId(ticket.getIdentityNumber()), trip.getTrain().getCode(), trip.getTrain().getName(),
				PopularRoute.placeName(trip.getRoute().getOriginStation().getName()),
				PopularRoute.placeName(trip.getRoute().getDestinationStation().getName()), departure, arrival,
				TripSearchService.durationLabel(Duration.between(departure, arrival)),
				(int) ChronoUnit.DAYS.between(departure.toLocalDate(), arrival.toLocalDate()),
				seat.getCoachNumber(), seat.getSeatCode(), SeatMapService.label(seat.getCoachType()),
				ticket.getTicketPrice(), ticket.getBooking().getBookingStatus() == BookingStatus.CANCELLED);
	}

}
