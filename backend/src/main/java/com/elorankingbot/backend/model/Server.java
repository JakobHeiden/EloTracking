package com.elorankingbot.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Document(collection = "server")
public class Server {

	@Id
	private long guildId;
	private Map<String, Game> gameNameToGame;
	private long adminRoleId;
	private long modRoleId;
	private long disputeCategoryId;
	private long leaderboardMessageId;
	private long leaderboardChannelId;
	private long resultChannelId;
	private boolean isMarkedForDeletion;
	private boolean hasSetupRoles;
	private boolean hasSetupGame;
	private boolean hasSetupMatchFinderModality;// TODO! in /help einflechten

	public Server(long guildId) {
		this.guildId = guildId;
		this.isMarkedForDeletion = false;
		this.gameNameToGame = new HashMap<>();
		this.adminRoleId = 0L;
		this.modRoleId = 0L;
	}

	public void addGame(Game game) {
		gameNameToGame.put(game.getName(), game);
	}

	public void removeGame(Game game) {
		gameNameToGame.remove(game.getName());
	}

	public Game getGame(String name) {
		return gameNameToGame.get(name);
	}

	public Collection<Game> getGames() {
		return gameNameToGame.values();
	}

	public boolean isPlayerEligibleForQueueInvite(Player player) {
		// not in group
		// not accepted other invite
		// TODO im zusammenhang mit premade queue
		return false;
	}

	public void removePlayerFromAllQueuesAndFormingGroups(Player player) {
		// TODO im zusammenhang mit premade queue
	}
}
