package com.elorankingbot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "rankingsentry")
@CompoundIndex(def = "{'guildId': 1, 'gameName': 1, 'rating': -1}")
@CompoundIndex(def = "{'guildId': 1, 'gameName': 1, 'playerTag': 1}")
public class RankingsEntry implements Comparable<RankingsEntry> {

	@Id
	@EqualsAndHashCode.Include
	private UUID id;
	private long guildId;
	private String gameName;
	private double rating;
	private String playerTag;
	private int wins, draws, losses;

	public RankingsEntry(Game game, Player player) {
		this.id = UUID.randomUUID();
		this.guildId = game.getGuildId();
		this.gameName = game.getName();
		PlayerGameStats playerGameStats = player.getOrCreatePlayerGameStats(game);
		this.rating = playerGameStats.getRating();
		this.playerTag = player.getTag();
		this.wins = playerGameStats.getWins();
		this.draws = playerGameStats.getDraws();
		this.losses = playerGameStats.getLosses();
	}

	@PersistenceConstructor
	public RankingsEntry(UUID id, long guildId, String gameName, double rating, String playerTag, int wins, int draws, int losses) {
		this.id = id;
		this.guildId = guildId;
		this.gameName = gameName;
		this.rating = rating;
		this.playerTag = playerTag;
		this.wins = wins;
		this.draws = draws;
		this.losses = losses;
	}

	@Override
	public int compareTo(RankingsEntry other) {
		return Double.compare(other.rating, this.rating);
	}
}
