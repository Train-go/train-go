package com.voyagego.traingo.model.response;

import java.time.LocalDate;

/**
 * One day of the date strip on the trip list.
 *
 * @param label    "Hôm nay", "Ngày mai", or the day of the week ("Thứ tư")
 * @param minPrice the lowest price among the day's trips with a free seat, in dong; null when there is none
 * @param soldOut  the day has trips but none has a free seat
 * @param hasTrips the day has at least one bookable trip
 * @param ariaLabel what a screen reader says for the day, such as "Thứ tư 21/10, giá từ 350.000đ"
 */
public record DateStripItem(LocalDate date, String label, Long minPrice, boolean soldOut, boolean hasTrips,
		String ariaLabel) {
}
