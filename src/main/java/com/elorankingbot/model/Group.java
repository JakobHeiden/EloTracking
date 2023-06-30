package com.elorankingbot.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Date;
import java.util.List;

@Data
public class Group implements Comparable<Group> {

	private List<Player> players;
	@DBRef(lazy = true)
	private Game game;
	private Date timestamp;

	public Group(List<Player> players, Game game) {
		this.players = players;
		this.game = game;
		this.timestamp = new Date();
	}

	@Override
	public int compareTo(Group other) {
		return (int) Math.ceil(this.getAverageRating() - other.getAverageRating());
	}

	public boolean hasPlayer(Player player) {
		return players.contains(player);// Player::equals compares playerId
	}

	public boolean updatePlayerIfPresent(Player player) {
		if (hasPlayer(player)) {
			players.set(players.indexOf(player), player);
			return true;
		} else {
			return false;
		}
	}

	public double getAverageRating() {
		double sumOfRatings = players.stream()
				.map(player -> player.getOrCreatePlayerGameStats(game).getRating())
				.reduce(0D, Double::sum);
		return sumOfRatings / players.size();
	}

	public double getRatingElasticity(Date now, MatchFinderQueue queue) {
		return (double) (now.getTime() - timestamp.getTime()) / (60 * 1000) * queue.getRatingElasticity();
	}
}
