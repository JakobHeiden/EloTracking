package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@UseToStringForLogging
public class MatchFinderQueue extends MatchFinderModality {

	public enum QueueType {
		SOLO,
		MIXED,
		PREMADE,
		NOT_A_TEAM_QUEUE
	}

	private final int numTeams;
	private final int playersPerTeam;
	private int minRating;
	private int maxRating;
	private QueueType queueType;
	private int maxPremadeSize;

	public MatchFinderQueue(Game game, String name, boolean allowDraw, int numTeams, int playersPerTeam,
							QueueType queueType, int maxPremadeSize) {
		super(game, name, allowDraw);
		this.numTeams = numTeams;
		this.playersPerTeam = playersPerTeam;
		this.queueType = queueType;
		this.maxPremadeSize = maxPremadeSize;
	}
}
