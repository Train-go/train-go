package com.voyagego.traingo.model.response;

/**
 * A route shown on the landing page, with its next departure and lowest price
 * taken from its upcoming trips. The two station codes build the card's link to
 * the trip list.
 */
public class PopularRoute {

	private final String originCode;
	private final String origin;
	private final String destinationCode;
	private final String destination;
	private final String note;
	private final StationPhoto photo;
	private final String durationLabel;
	private final String nextLabel;
	private final long price;

	/**
	 * @param photo the destination's, or null when there is none
	 * @param nextLabel "Hôm nay, 08:00", the next departure
	 * @param price the lowest price of the upcoming trips
	 */
	public PopularRoute(String originCode, String origin, String destinationCode, String destination, String note,
			StationPhoto photo, String durationLabel, String nextLabel, long price) {
		this.originCode = originCode;
		this.origin = origin;
		this.destinationCode = destinationCode;
		this.destination = destination;
		this.note = note;
		this.photo = photo;
		this.durationLabel = durationLabel;
		this.nextLabel = nextLabel;
		this.price = price;
	}

	public String getDurationLabel() {
		return durationLabel;
	}

	public String getNextLabel() {
		return nextLabel;
	}

	public long getPrice() {
		return price;
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
