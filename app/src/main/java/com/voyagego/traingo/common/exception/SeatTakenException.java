package com.voyagego.traingo.common.exception;

import java.util.List;

/**
 * Seats the customer chose were booked by someone else before "Tiếp tục".
 * Nothing is locked while choosing (ADR 0010), so this is expected, not a
 * fault: the page shows the sentence and keeps the other chosen seats.
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

	public List<String> getSeatCodes() {
		return seatCodes;
	}

	public List<Long> getSeatIds() {
		return seatIds;
	}

}
