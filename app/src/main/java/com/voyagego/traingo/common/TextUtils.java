package com.voyagego.traingo.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Text helpers shared by services. */
public final class TextUtils {

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private TextUtils() {
	}

	/**
	 * Lower case without Vietnamese tone and vowel marks, so a search for "hue"
	 * or "HUE" matches "Huế". NFD does not split đ, so it is replaced on its own.
	 * Null becomes the empty string.
	 */
	public static String fold(String text) {
		if (text == null) {
			return "";
		}
		String bare = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		return bare.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
	}

}
