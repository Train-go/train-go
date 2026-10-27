package com.voyagego.traingo.common.exception;

import java.util.ArrayList;
import java.util.List;

import com.voyagego.traingo.entity.TripSeat;

/**
 * Seats the customer chose were booked by someone else, found either at
 * "Tiếp tục" on the seat page or at payment. Nothing is locked while choosing
 * (ADR 0010), so this is expected, not a fault: the page shows the sentence and
 * keeps the other chosen seats.
 */
public class SeatTakenException extends BusinessRuleException {

	private final List<String> seatCodes;
	private final List<Long> seatIds;

	/**
	 * @param seatCodes how each taken seat is named in the sentence, such as "A05"
	 * @param seatIds   the same seats' ids, so the page can drop them from the choice
	 */
	public SeatTakenException(List<String> seatCodes, List<Long> seatIds) {
		super((seatCodes.size() == 1 ? "Chỗ " : "Các chỗ ") + String.join(", ", seatCodes)
				+ " vừa được người khác đặt. Vui lòng chọn chỗ khác.");
		this.seatCodes = List.copyOf(seatCodes);
		this.seatIds = List.copyOf(seatIds);
	}

	/**
	 * For the seats found taken. A seat code repeats across coaches (A05 is in
	 * coach 1 and 2), so a code that would be ambiguous in the sentence carries
	 * its coach.
	 */
	public static SeatTakenException of(List<TripSeat> taken) {
		List<String> labels = new ArrayList<>();
		for (TripSeat seat : taken) {
			boolean repeated = taken.stream().filter(other -> other.getSeatCode().equals(seat.getSeatCode())).count() > 1;
			labels.add(repeated ? seat.getSeatCode() + " (toa " + seat.getCoachNumber() + ")" : seat.getSeatCode());
		}
		return new SeatTakenException(labels, taken.stream().map(TripSeat::getId).toList());
	}

	public List<String> getSeatCodes() {
		return seatCodes;
	}

	public List<Long> getSeatIds() {
		return seatIds;
	}

}
