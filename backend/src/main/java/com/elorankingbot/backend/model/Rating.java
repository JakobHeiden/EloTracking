package com.elorankingbot.backend.model;

import java.util.List;
import java.util.UUID;

public class Rating implements Comparable<Rating> {

	private final Game game;
	private double rating;
	private int wins;
	private int draws;
	private int losses;
	private List<UUID> matchHistory;

	public Rating(Game game, double rating) {
		this.game = game;
		this.rating = rating;
		this.wins = 0;
		this.draws = 0;
		this.losses = 0;
	}

	public void addWin() {
		wins++;
	}

	public void addDraw() {
		draws++;
	}

	public void addLoss() {
		losses++;
	}

	@Override
	public int compareTo(Rating otherRating) {
		return Double.compare(this.rating, otherRating.rating);
	}
}
