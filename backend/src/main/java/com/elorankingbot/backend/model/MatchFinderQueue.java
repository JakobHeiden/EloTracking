package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.ArrayList;
import java.util.List;

@Data
@UseToStringForLogging
public class MatchFinderQueue {

	public enum QueueType {
		SOLO,
		MIXED,
		PREMADE
	}

	@Id
	private final String name;
	private List<Group> groups;
	private List<Group> formingGroups;
	@DBRef(lazy = true)
	private final Game game;
	private final int numTeams;
	private final int numPlayersPerTeam;
	private int minRating;
	private int maxRating;
	private final QueueType queueType;
	private final int maxPremadeSize;
	private boolean isBuildMatchFromTopPlayer;

	public MatchFinderQueue(Game game, String name, int numTeams, int numPlayersPerTeam,
							QueueType queueType, int maxPremadeSize) {
		this.game = game;
		this.name = name;
		this.numTeams = numTeams;
		this.numPlayersPerTeam = numPlayersPerTeam;
		this.queueType = queueType;
		this.maxPremadeSize = maxPremadeSize;
		this.groups = new ArrayList<>();
		this.formingGroups = new ArrayList<>();
		this.isBuildMatchFromTopPlayer = true;
	}

	public void addGroup(Group group) {
		groups.add(group);
	}

	public void removeGroupsContainingPlayer(Player player) {
		groups.removeIf(group -> group.hasPlayer(player));
		// TODO!
	}

	public int getNumPlayers() {
		return numTeams * numPlayersPerTeam;
	}

	public String getDescription() {
		if (numTeams == 2) {
			if (numPlayersPerTeam == 1) return "Join this 1v1 queue";
			else {
				return switch (queueType) {
					case SOLO -> String.format("Join this %sv%s queue for solo players", numPlayersPerTeam, numPlayersPerTeam);
					case PREMADE -> String.format("Join this %sv%s queue for full premade teams", numPlayersPerTeam, numPlayersPerTeam);
					case MIXED -> String.format("Join this %sv%s queue for solo players " +
									"and premade teams no larger than %s players",
							numPlayersPerTeam, numPlayersPerTeam, maxPremadeSize);
				};
			}
		} else {
			if (numPlayersPerTeam == 1) return String.format("Join this %s way free for all queue", numTeams);
			else {
				return switch (queueType) {
					case SOLO -> String.format("Join this teams of %s, %s way free for all queue for solo players",
							numPlayersPerTeam, numTeams);
					case PREMADE -> String.format("Join this teams of %s, %s way free for all queue for full premade teams",
							numPlayersPerTeam, numTeams);
					case MIXED -> String.format("Join this teams of %s, %s way free for all queue for solo players " +
									"and premade teams < %s players",
							numPlayersPerTeam, numTeams, maxPremadeSize);
				};
			}
		}
	}
}
