package com.voyagego.traingo.model.response;

/**
 * A photo of the place a station serves, stored under static/images/home/.
 *
 * @param path the full-size file, for the large journey card on a high-density screen
 * @param smallPath a copy for the small cards and thumbnails
 * @param srcset both files with their widths, for an img srcset; the page's sizes attribute picks one
 */
public record StationPhoto(String path, String smallPath, String srcset, String alt) {

	private static final String DIR = "/images/home/";

	/**
	 * For a station without a photo of its own: the train on the Hải Vân pass, the home page's banner. It has no
	 * small copy, so a small card loads the banner itself, which the home page has already fetched.
	 */
	public static final StationPhoto DEFAULT = new StationPhoto(DIR + "hero-hai-van.jpg", DIR + "hero-hai-van.jpg",
			DIR + "hero-hai-van.jpg 2880w", "Tàu trên đèo Hải Vân");

	/** A station's own photo: "tuyen-hni.jpg", 2800px wide, beside its 800px copy "tuyen-hni-800.jpg". */
	public static StationPhoto of(String file, String alt) {
		String small = DIR + file.replaceFirst("\\.jpg$", "-800.jpg");
		return new StationPhoto(DIR + file, small, small + " 800w, " + DIR + file + " 2800w", alt);
	}

}
