package com.voyagego.traingo.model.response;

import java.time.LocalDate;

/**
 * One day of the date strip on the trip list.
 *
 * @param label "Hôm nay", "Ngày mai", or the day of the week ("Thứ tư")
 */
public record DateTab(LocalDate date, String label) {
}
