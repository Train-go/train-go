package com.voyagego.traingo.model.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;

import com.voyagego.traingo.entity.CoachType;

import jakarta.validation.constraints.AssertTrue;

/**
 * The query string of the trip list, /trips. Everything the page shows comes
 * from here, so a copied link opens the same search, filters and order.
 *
 * Station codes are upper-cased on the way in, so /trips?from=hue works too.
 * Unknown slot and sort values are ignored rather than refused: they only
 * narrow or order a list, and a stale link should still open.
 */
public class TripSearchRequest {

	public static final String SORT_EARLIEST = "earliest";
	public static final String SORT_CHEAPEST = "cheapest";
	public static final String SORT_FASTEST = "fastest";

	public static final String SLOT_MORNING = "morning";
	public static final String SLOT_AFTERNOON = "afternoon";
	public static final String SLOT_EVENING = "evening";
	public static final String SLOT_NIGHT = "night";
	private static final List<String> SLOTS = List.of(SLOT_MORNING, SLOT_AFTERNOON, SLOT_EVENING, SLOT_NIGHT);

	private String from;

	private String to;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate date;

	/** Departure windows: morning, afternoon, evening, night. */
	private List<String> slot = new ArrayList<>();

	/** Keep trips with at least one free seat of any of these types. */
	private List<CoachType> type = new ArrayList<>();

	private boolean hideSoldOut;

	private String sort;

	/** Both stations are given, so there is something to search. */
	public boolean hasRoute() {
		return from != null && to != null;
	}

	@AssertTrue(message = "Ga đến phải khác ga đi.")
	public boolean isDifferentStations() {
		return !hasRoute() || !from.equals(to);
	}

	/** One of the three orders, earliest when the link names none or an unknown one. */
	public String sortKey() {
		return SORT_CHEAPEST.equals(sort) || SORT_FASTEST.equals(sort) ? sort : SORT_EARLIEST;
	}

	public boolean hasSlot(String value) {
		return slot.contains(value);
	}

	public boolean hasType(CoachType value) {
		return type.contains(value);
	}

	/** Number of filters switched on, for the badge on the phone's "Lọc chuyến" button. */
	public int filterCount() {
		return slot.size() + type.size() + (hideSoldOut ? 1 : 0);
	}

	/** This search on another day, filters and order kept. */
	public String url(LocalDate day) {
		return build(day, sort, true);
	}

	/** This search in another order. */
	public String sortUrl(String order) {
		return build(date, order, true);
	}

	/** This search with every filter off, order kept. */
	public String clearFiltersUrl() {
		return build(date, sort, false);
	}

	private String build(LocalDate day, String order, boolean keepFilters) {
		UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/trips")
				.queryParam("from", from)
				.queryParam("to", to);
		if (day != null) {
			uri.queryParam("date", day);
		}
		if (keepFilters) {
			slot.forEach(value -> uri.queryParam("slot", value));
			type.forEach(value -> uri.queryParam("type", value));
			if (hideSoldOut) {
				uri.queryParam("hideSoldOut", true);
			}
		}
		if (order != null && !SORT_EARLIEST.equals(order)) {
			uri.queryParam("sort", order);
		}
		return uri.encode().toUriString();
	}

	private static String code(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = code(from);
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = code(to);
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public List<String> getSlot() {
		return slot;
	}

	public void setSlot(List<String> slot) {
		this.slot = slot == null ? new ArrayList<>()
				: new ArrayList<>(slot.stream().filter(SLOTS::contains).distinct().toList());
	}

	public List<CoachType> getType() {
		return type;
	}

	public void setType(List<CoachType> type) {
		this.type = type == null ? new ArrayList<>()
				: new ArrayList<>(type.stream().filter(Objects::nonNull).distinct().toList());
	}

	public boolean isHideSoldOut() {
		return hideSoldOut;
	}

	public void setHideSoldOut(boolean hideSoldOut) {
		this.hideSoldOut = hideSoldOut;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

}
