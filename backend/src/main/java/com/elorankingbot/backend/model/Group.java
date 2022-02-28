package com.elorankingbot.backend.model;

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
		boolean isSameSize = this.players.size() == other.players.size();
		if (isSameSize) return this.timestamp.compareTo(other.timestamp);
		else return this.players.size() - other.players.size();
	}

	public boolean hasPlayer(Player player) {
		return players.contains(player);
	}
}
