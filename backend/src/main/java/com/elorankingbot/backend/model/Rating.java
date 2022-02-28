package com.elorankingbot.backend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.List;
import java.util.UUID;

@Data
public class Rating implements Comparable<Rating> {

	@DBRef(lazy = true)
	private final Game game;
	private double value;
	private int wins;
	private int draws;
	private int losses;
	private List<UUID> matchHistory;
	private List<PlayerMatchResult> recentMatches;

	public Rating(Game game, double value) {
		this.game = game;
		this.value = value;
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
		return Double.compare(this.value, otherRating.value);
	}
}
