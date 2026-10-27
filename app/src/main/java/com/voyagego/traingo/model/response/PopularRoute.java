package com.voyagego.traingo.model.response;

/**
 * A route shown on the landing page. Carries no price or duration: those
 * come from a real Trip, and inventing them here would put numbers on screen
 * that no one can trace back to data. The two station codes build the card's
 * link to the trip list.
 */
public class PopularRoute {

	private final String originCode;
	private final String origin;
	private final String destinationCode;
	private final String destination;
	private final String note;
	private final StationPhoto photo;

	/** {@code photo} is the destination's, or null when there is none. */
	public PopularRoute(String originCode, String origin, String destinationCode, String destination, String note,
			StationPhoto photo) {
		this.originCode = originCode;
		this.origin = origin;
		this.destinationCode = destinationCode;
		this.destination = destination;
		this.note = note;
		this.photo = photo;
	}

	public String getOriginCode() {
		return originCode;
	}

	public String getOrigin() {
		return origin;
	}

	public String getDestinationCode() {
		return destinationCode;
	}

	public String getDestination() {
		return destination;
	}

	public String getNote() {
		return note;
	}

	public StationPhoto getPhoto() {
		return photo;
	}

	/** "Hà Nội" for "Ga Hà Nội": the card reads as a trip between two places. */
	public String getOriginPlace() {
		return placeName(origin);
	}

	public String getDestinationPlace() {
		return placeName(destination);
	}

	public static String placeName(String stationName) {
		return stationName.startsWith("Ga ") ? stationName.substring(3) : stationName;
	}

}
