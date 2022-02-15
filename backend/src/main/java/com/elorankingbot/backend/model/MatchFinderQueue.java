package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@UseToStringForLogging
@Document(collection = "matchfinderqueue")
public class MatchFinderQueue extends MatchFinderModality {

	private final int numTeams;
	private final int playerPerTeam;
	private int minRating;
	private int maxRating;

	public MatchFinderQueue(String name, Game game, int numTeams, int playerPerTeam) {
		super(name, game);
		this.numTeams = numTeams;
		this.playerPerTeam = playerPerTeam;
	}
}
