package com.voyagego.traingo.home.model;

/**
 * A route shown on the landing page. Carries no price or duration: those
 * come from a real Trip, and inventing them here would put numbers on screen
 * that no one can trace back to data.
 */
public class PopularRoute {

	private final String origin;
	private final String destination;
	private final String note;

	public PopularRoute(String origin, String destination, String note) {
		this.origin = origin;
		this.destination = destination;
		this.note = note;
	}

	public String getOrigin() {
		return origin;
	}

	public String getDestination() {
		return destination;
	}

	public String getNote() {
		return note;
	}

}
