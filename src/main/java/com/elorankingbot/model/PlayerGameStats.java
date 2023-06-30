package com.elorankingbot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
// TODO this class should probably be replaced with a db reference to RankingsEntry
public class PlayerGameStats implements Comparable<PlayerGameStats> {

	private double rating;
	private int wins, losses, draws, cancels;
	private List<UUID> matchHistory;// TODO das hier vllt als referenz ablegen? das player-objekt wird ja immer groesser,
	// die matchhistory wird aber nur manchmal gebraucht...

	public PlayerGameStats(double rating) {
		this.rating = rating;
		this.wins = 0;
		this.draws = 0;
		this.losses = 0;
		this.cancels = 0;
		this.matchHistory = new ArrayList<>();
	}

	public void addResultStatus(ReportStatus resultStatus) {
		switch (resultStatus) {
			case WIN -> wins++;
			case LOSE -> losses++;
			case DRAW -> draws++;
			case CANCEL -> cancels++;
		}
	}

	public void subtractResultStatus(ReportStatus resultStatus) {
		switch (resultStatus) {
			case WIN -> wins--;
			case LOSE -> losses--;
			case DRAW -> draws--;
			case CANCEL -> cancels--;
		}
	}

	@Override
	public int compareTo(PlayerGameStats otherPlayerGameStats) {
		return Double.compare(this.rating, otherPlayerGameStats.rating);
	}
}
