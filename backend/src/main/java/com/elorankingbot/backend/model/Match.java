package com.elorankingbot.backend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
@Document
public class Match {

	private UUID id;
	@DBRef(lazy = true)
	private Game game;
	private List<List<Player>> players;

	public Match(Game game, List<List<Player>> players) {
		this.id = UUID.randomUUID();
		this.game = game;
		this.players = players;
	}
}
