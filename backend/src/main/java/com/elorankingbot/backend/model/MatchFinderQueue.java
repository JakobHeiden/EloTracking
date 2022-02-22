package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Set;
import java.util.TreeSet;

@Data
@ToString
@UseToStringForLogging
public class MatchFinderQueue {

	public enum QueueType {
		SOLO,
		MIXED,
		PREMADE,
		NOT_A_TEAM_QUEUE
	}

	private String name;
	@DBRef(lazy = true)
	private Game game;
	private final int numTeams;
	private final int playersPerTeam;
	private int minRating;
	private int maxRating;
	private QueueType queueType;
	private int maxPremadeSize;
	private Set<Group> groups;
	private boolean isBuildMatchFromTopPlayer;

	public MatchFinderQueue(Game game, String name, int numTeams, int playersPerTeam,
							QueueType queueType, int maxPremadeSize) {
		this.game = game;
		this.name = name;
		this.numTeams = numTeams;
		this.playersPerTeam = playersPerTeam;
		this.queueType = queueType;
		this.maxPremadeSize = maxPremadeSize;
		this.groups = new TreeSet<>();
		this.isBuildMatchFromTopPlayer = true;
	}

	public void addGroup(Group group) {
		groups.add(group);
	}

	public String getDescription() {
		if (numTeams == 2) {
			if (playersPerTeam == 1) return "Join this 1v1 queue";
			else {
				switch (queueType) {
					case SOLO:
						return String.format("Join this %sv%s queue for solo players", playersPerTeam, playersPerTeam);
					case PREMADE:
						return String.format("Join this %sv%s queue for full premade teams", playersPerTeam, playersPerTeam);
					case MIXED:
						return String.format("Join this %sv%s queue for solo players " +
										"and premade teams no larger than %s players",
								playersPerTeam, playersPerTeam, maxPremadeSize);
				}
			}
		} else {
			if (playersPerTeam == 1) return String.format("Join this %s way free for all queue", numTeams);
			else {
				switch (queueType) {
					case SOLO:
						return String.format("Join this teams of %s, %s way free for all queue for solo players",
								playersPerTeam, numTeams);
					case PREMADE:
						return String.format("Join this teams of %s, %s way free for all queue for full premade teams",
								playersPerTeam, numTeams);
					case MIXED:
						return String.format("Join this teams of %s, %s way free for all queue for solo players " +
										"and premade teams < %s players",
								playersPerTeam, numTeams, maxPremadeSize);
				}
			}
		}
		return "error parsing queue data";
	}
}
