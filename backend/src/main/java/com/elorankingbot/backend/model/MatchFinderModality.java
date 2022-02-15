package com.elorankingbot.backend.model;

import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@NoArgsConstructor
public abstract class MatchFinderModality {

	@Id
	private UUID uuid;
	private Game game;
	private String name;
	private int matchAutoResolveTime;
	private int messageCleanupTime;
	private int noReportsModalityDecayTime;

	public MatchFinderModality(String name, Game game) {
		this.uuid = UUID.randomUUID();
		this.game = game;
		this.name = name;
		this.matchAutoResolveTime = 24 * 60;
		this.messageCleanupTime = 12 * 60;
		this.noReportsModalityDecayTime = 7 * 24 * 60;
	}
}
