package com.elorankingbot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatTools {

	public static String formatRating(double rating) {
		return String.format("%.1f", Float.valueOf(Math.round(rating * 10)) / 10);
	}

	public static String formatRatingChange(double ratingChange) {
		if (ratingChange < 0) return formatRating(ratingChange);
		else return "+" + formatRating(ratingChange);
	}

	public static boolean isLegalDiscordName(String string) {
		if (!string.toLowerCase().equals(string)) return false;
		Pattern p = Pattern.compile("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}$");
		Matcher m = p.matcher(string);
		return m.find();
	}

	public static String illegalNameMessage() {
		return "Illegal name. Please use only lowercase letters, digits, dash, and underscore.";
	}
}
