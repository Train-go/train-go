package com.voyagego.traingo.model.response;

/**
 * A photo of the place a station serves, stored under static/images/home/.
 * Where each photo comes from and its licence are listed in the homepage
 * footer (templates/fragments/footer.html), which every licence requires.
 */
public record StationPhoto(String file, String alt) {

	public String path() {
		return "/images/home/" + file;
	}

}
