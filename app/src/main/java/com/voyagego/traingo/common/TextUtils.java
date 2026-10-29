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


	/**
	 * Hides a phone number except its last four digits: "0901000001" becomes
	 * "•••• 0001". Null when there is no number to show.
	 */
	public static String maskPhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return null;
		}
		String digits = phone.trim();
		return digits.length() <= 4 ? "••••" : "•••• " + digits.substring(digits.length() - 4);
	}

	/**
	 * The letters shown in a name's avatar: the first letter of the last two
	 * words, so "Nguyễn Văn An" gives "VA" and "An" gives "A".
	 */
	public static String initials(String name) {
		if (name == null || name.isBlank()) {
			return "?";
		}
		String[] words = name.trim().split("\\s+");
		String last = words[words.length - 1];
		String initials = words.length > 1 ? words[words.length - 2].substring(0, 1) + last.substring(0, 1)
				: last.substring(0, 1);
		return initials.toUpperCase(Locale.ROOT);
	}

	/**
	 * A price in the short form of the date strip: 350000 gives "350k",
	 * 1200000 gives "1,2tr", 2000000 gives "2tr". Under 1.000 dong it is
	 * printed as it is ("500đ").
	 */
	public static String shortMoney(long dong) {
		if (dong < 1_000) {
			return dong + "đ";
		}
		long thousands = Math.round(dong / 1_000.0);
		if (thousands < 1_000) {
			return thousands + "k";
		}
		long tenths = Math.round(dong / 100_000.0);
		return tenths % 10 == 0 ? tenths / 10 + "tr" : tenths / 10 + "," + tenths % 10 + "tr";
	}

}
