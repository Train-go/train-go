package com.voyagego.traingo.model.response;

import java.time.LocalDate;
import java.util.List;

/**
 * The journeys of the homepage: the routes of one region, ranked, the first
 * {@code limit} of them, and what the footer and the date band take from the
 * overall ranking.
 *
 * @param region the open chip, "" for all
 * @param cta the top route of all, or null when no route has a coming trip
 */
public record PopularView(String region, int limit, boolean hasMore, List<Chip> chips, List<PopularRoute> routes,
		List<PopularRoute> quickRoutes, List<PopularRoute> footerRoutes, PopularRoute cta, List<Day> days) {

	public record Chip(String key, String label) {
	}

	/** {@code label} is "Hôm nay", "T4"...; {@code dateLabel} "27/10". */
	public record Day(LocalDate date, String label, String dateLabel) {
	}

}
