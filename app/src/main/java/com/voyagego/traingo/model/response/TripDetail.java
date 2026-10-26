package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * Everything the trip sheet (static/js/admin/trip-sheet.js) shows, as JSON
 * from GET /api/admin/trips/{id}. Dates and labels are formatted by
 * TripAdminService so the sheet only places text. Only CONFIRMED bookings
 * count as sold.
 */
public class TripDetail {

	private final long id;
	private final long trainId;
	private final String trainCode;
	private final String trainName;
	private final String originName;
	private final String destinationName;
	private final String phase;
	private final String phaseLabel;
	/** For example "Thứ Hai, 26/10/2026 · 08:00 → 16:45". */
	private final String schedule;
	/** For example "08:00 26/10", for the cancel question. */
	private final String departureShort;
	private final int distanceKm;
	/** For example "8 giờ 45 phút". */
	private final String duration;
	private final long basePrice;
	private final int soldSeats;
	private final int totalSeats;
	private final long bookedCount;
	private final boolean cancelled;
	/** Null when the trip can be cancelled. */
	private final String cancelBlockReason;
	private final List<CoachView> coaches;
	private final List<PassengerRow> passengers;

	public TripDetail(long id, long trainId, String trainCode, String trainName, String originName,
			String destinationName, String phase, String phaseLabel, String schedule, String departureShort,
			int distanceKm, String duration, long basePrice, int soldSeats, int totalSeats, long bookedCount,
			boolean cancelled, String cancelBlockReason, List<CoachView> coaches, List<PassengerRow> passengers) {
		this.id = id;
		this.trainId = trainId;
		this.trainCode = trainCode;
		this.trainName = trainName;
		this.originName = originName;
		this.destinationName = destinationName;
		this.phase = phase;
		this.phaseLabel = phaseLabel;
		this.schedule = schedule;
		this.departureShort = departureShort;
		this.distanceKm = distanceKm;
		this.duration = duration;
		this.basePrice = basePrice;
		this.soldSeats = soldSeats;
		this.totalSeats = totalSeats;
		this.bookedCount = bookedCount;
		this.cancelled = cancelled;
		this.cancelBlockReason = cancelBlockReason;
		this.coaches = List.copyOf(coaches);
		this.passengers = List.copyOf(passengers);
	}

	public long getId() { return id; }
	public long getTrainId() { return trainId; }
	public String getTrainCode() { return trainCode; }
	public String getTrainName() { return trainName; }
	public String getOriginName() { return originName; }
	public String getDestinationName() { return destinationName; }
	public String getPhase() { return phase; }
	public String getPhaseLabel() { return phaseLabel; }
	public String getSchedule() { return schedule; }
	public String getDepartureShort() { return departureShort; }
	public int getDistanceKm() { return distanceKm; }
	public String getDuration() { return duration; }
	public long getBasePrice() { return basePrice; }
	public int getSoldSeats() { return soldSeats; }
	public int getTotalSeats() { return totalSeats; }
	public long getBookedCount() { return bookedCount; }
	public boolean isCancelled() { return cancelled; }
	public String getCancelBlockReason() { return cancelBlockReason; }
	public List<CoachView> getCoaches() { return coaches; }
	public List<PassengerRow> getPassengers() { return passengers; }

	/** One coach tab of the seat map. */
	public static final class CoachView {

		private final int number;
		private final String type;
		private final String typeLabel;
		private final int capacity;
		private final int sold;
		private final List<SeatView> seats;

		public CoachView(int number, String type, String typeLabel, int capacity, int sold, List<SeatView> seats) {
			this.number = number;
			this.type = type;
			this.typeLabel = typeLabel;
			this.capacity = capacity;
			this.sold = sold;
			this.seats = List.copyOf(seats);
		}

		public int getNumber() { return number; }
		/** SEAT or SLEEPER. */
		public String getType() { return type; }
		public String getTypeLabel() { return typeLabel; }
		public int getCapacity() { return capacity; }
		public int getSold() { return sold; }
		public List<SeatView> getSeats() { return seats; }
	}

	/** One seat or bed. Passenger and booking are null while it is free. */
	public static final class SeatView {

		private final String code;
		private final Integer cabin;
		private final boolean sold;
		private final String passengerName;
		private final String bookingCode;

		public SeatView(String code, Integer cabin, boolean sold, String passengerName, String bookingCode) {
			this.code = code;
			this.cabin = cabin;
			this.sold = sold;
			this.passengerName = passengerName;
			this.bookingCode = bookingCode;
		}

		public String getCode() { return code; }
		/** Beds only; null for seats. */
		public Integer getCabin() { return cabin; }
		public boolean isSold() { return sold; }
		public String getPassengerName() { return passengerName; }
		public String getBookingCode() { return bookingCode; }
	}

	/** One ticket of a confirmed booking. The identity number is masked before it leaves the server. */
	public static final class PassengerRow {

		private final String fullName;
		private final String maskedIdentity;
		private final int coachNumber;
		private final String seatCode;
		private final String bookingCode;
		private final long ticketPrice;

		public PassengerRow(String fullName, String maskedIdentity, int coachNumber, String seatCode,
				String bookingCode, long ticketPrice) {
			this.fullName = fullName;
			this.maskedIdentity = maskedIdentity;
			this.coachNumber = coachNumber;
			this.seatCode = seatCode;
			this.bookingCode = bookingCode;
			this.ticketPrice = ticketPrice;
		}

		public String getFullName() { return fullName; }
		public String getMaskedIdentity() { return maskedIdentity; }
		public int getCoachNumber() { return coachNumber; }
		public String getSeatCode() { return seatCode; }
		public String getBookingCode() { return bookingCode; }
		public long getTicketPrice() { return ticketPrice; }
	}

}
