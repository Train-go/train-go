package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * The customer's /my-trips page: the cards of the selected group and the size
 * of each of the three groups (D8), every label ready to print.
 *
 * @param tab upcoming, past or cancelled
 */
public record MyTripsView(String tab, long upcomingCount, long pastCount, long cancelledCount,
		List<BookingCard> cards) {

	/**
	 * One booking as a card that links to its detail page.
	 *
	 * @param departureLabel "Thứ Năm, 29/10/2026 · 08:00"
	 * @param seatsLabel "Toa 1 · A05, Toa 3 · B02"
	 * @param statusCss the .tg-status modifier: paid or cancelled
	 */
	public record BookingCard(long id, String code, String routeLabel, String departureLabel, String trainCode,
			String seatsLabel, int ticketCount, long totalPrice, String statusLabel, String statusCss) {
	}

}
