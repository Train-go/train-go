package com.voyagego.traingo.model.response;

import java.util.List;

/**
 * The trip list of one day after filters and sorting.
 *
 * @param dayTotal bookable trips that day before any filter, so the page can
 *                 tell "no trip that day" from "no trip matches the filters"
 */
public record TripSearchResult(List<TripCard> cards, int dayTotal) {
}
