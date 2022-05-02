package com.elorankingbot.backend.components;

public class FormatTools {

	public static String formatRating(double rating) {
		return String.format("%.1f", Float.valueOf(Math.round(rating * 10)) / 10);
	}

	public static String formatRatingChange(double ratingChange) {
		if (ratingChange < 0) return formatRating(ratingChange);
		else return "+" + formatRating(ratingChange);
	}
}
