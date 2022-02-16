package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@NoArgsConstructor
@Data
@ToString
@UseToStringForLogging
public abstract class MatchFinderModality {

	private String name;
	private Game game;
	private boolean allowDraw;
	private int matchAutoResolveTime;
	private int messageCleanupTime;
	private int noReportsModalityDecayTime;

	public MatchFinderModality(Game game, String name, boolean allowDraw) {
		this.name = name;
		this.game = game;
		this.allowDraw = allowDraw;
		this.matchAutoResolveTime = 24 * 60;
		this.messageCleanupTime = 12 * 60;
		this.noReportsModalityDecayTime = 7 * 24 * 60;
	}
}
