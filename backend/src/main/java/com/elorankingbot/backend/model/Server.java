package com.elorankingbot.backend.model;

import com.elorankingbot.backend.service.DiscordBotService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Document(collection = "server")
public class Server {

	@Id
	@EqualsAndHashCode.Include
	private long guildId;
	private Map<String, Game> gameNameToGame;
	private long adminRoleId, modRoleId;
	private List<Long> archiveCategoryIds;
	private long disputeCategoryId, matchCategoryId;
	private boolean isMarkedForDeletion;

	public Server(long guildId) {
		this.guildId = guildId;
		this.isMarkedForDeletion = false;
		this.gameNameToGame = new HashMap<>();
		this.archiveCategoryIds = new ArrayList<>();
		this.adminRoleId = 0L;
		this.modRoleId = 0L;
	}

	public void addGame(Game game) {
		gameNameToGame.put(game.getName().toLowerCase(), game);
	}

	public void removeGame(Game game) {
		gameNameToGame.remove(game.getName().toLowerCase());
	}

	public Game getGame(String name) {
		return gameNameToGame.get(name.toLowerCase());
	}

	public List<Game> getGames() {
		return gameNameToGame.values().stream().toList();
	}

	public List<MatchFinderQueue> getQueues() {
		return getGames().stream().flatMap(game -> game.getQueues().stream()).toList();
	}

	public String getName(DiscordBotService bot) {
		return bot.getGuildById(guildId).block().getName();
	}
}
