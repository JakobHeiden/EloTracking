package com.elorankingbot.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Document(collection = "server")
public class Server {

	@Id
	private long guildId;
	private Map<String, Game> games;
	private long adminRoleId;
	private long modRoleId;
	private long disputeCategoryId;
	private boolean isMarkedForDeletion;
	private boolean hasSetupRoles;
	private boolean hasSetupGame;
	private boolean hasSetupMatchFinderModality;// TODO! in /help einflechten

	public Server(long guildId) {
		this.guildId = guildId;
		this.isMarkedForDeletion = false;
		this.games = new HashMap<>();
		this.adminRoleId = 0L;
		this.modRoleId = 0L;
	}

	public void addGame(Game game) {
		games.put(game.getName(), game);
	}

	public void removeGame(Game game) {
		games.remove(game.getName());
	}
}
