package com.elorankingbot.timedtask;

import com.google.common.primitives.Ints;

import java.util.Optional;

public class DurationParser {

	public static Optional<Integer> parse(String string) {
		string = string.replace(" ", "");
		if (parseForInt(string).isPresent()) {
			if (parseForInt(string).get() < 0) return Optional.empty();
			else return parseForInt(string);
		}

		if (string.length() < 2) return Optional.empty();
		String intString = string.substring(0, string.length() - 1);
		String timeUnitSTring = string.substring(string.length() - 1);
		if (parseForInt(intString).isEmpty()) return Optional.empty();

		int number = parseForInt(intString).get();
		if (number < 0) return Optional.empty();

		switch (timeUnitSTring) {
			case "m":
				return Optional.of(number);
			case "h":
				return Optional.of(number * 60);
			case "d":
				return Optional.of(number * 60 * 24);
			case "w":
				return Optional.of(number * 60 * 24 * 7);
			default:
				return Optional.empty();
		}
	}

	private static Optional<Integer> parseForInt(String string) {
		Integer maybeInt = Ints.tryParse(string);
		if (maybeInt == null) return Optional.empty();
		else return Optional.of(maybeInt);
	}

	public static String minutesToString(int totalMins) {
		if (totalMins <= 0) return "error: number not positive";

		int weeks = totalMins / (60 * 24 * 7);
		totalMins -= weeks * 60 * 24 * 7;
		int days = totalMins / (60 * 24);
		totalMins -= days * 60 * 24;
		int hours = totalMins / 60;
		int mins = totalMins - hours * 60;

		String weeksString = "";
		if (weeks == 1) weeksString = "1 week, ";
		if (weeks > 1) weeksString = weeks + " weeks, ";
		String daysString = "";
		if (days == 1) daysString = "1 day, ";
		if (days > 1) daysString = days + " days, ";
		String hoursString = "";
		if (hours == 1) hoursString = "1 hour, ";
		if (hours > 1) hoursString = hours + " hours, ";
		String minutesString = "";
		if (mins == 1) minutesString = "1 minuteXX";
		if (mins > 1) minutesString = mins + " minutesXX";
		String returnString = String.format("%s%s%s%s", weeksString, daysString, hoursString, minutesString);
		return returnString.substring(0, returnString.length() - 2);
	}
}
