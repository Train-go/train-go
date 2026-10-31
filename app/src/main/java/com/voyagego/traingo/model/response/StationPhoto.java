package com.voyagego.traingo.model.response;

/**
 * A photo of the place a station serves, stored under static/images/home/.
 */
public record StationPhoto(String file, String alt) {

	public String path() {
		return "/images/home/" + file;
	}

}
