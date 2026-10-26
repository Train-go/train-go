package com.voyagego.traingo.common;

import java.text.Normalizer;
import java.util.Locale;

/** Small string helpers shared by services. */
public final class TextUtils {

	private TextUtils() {
	}

	/**
	 * Lower-cases and strips Vietnamese diacritics so that "Đà Nẵng", "da nang"
	 * and "DA NANG" compare equal. Used by every admin search box.
	 */
	public static String fold(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT);
	}

	/**
	 * Hides an identity number except its last four characters:
	 * "079095001234" becomes "•••• 1234". Shorter values are fully hidden.
	 */
	public static String maskId(String idNumber) {
		if (idNumber == null || idNumber.length() <= 4) {
			return "••••";
		}
		return "•••• " + idNumber.substring(idNumber.length() - 4);
	}

}
