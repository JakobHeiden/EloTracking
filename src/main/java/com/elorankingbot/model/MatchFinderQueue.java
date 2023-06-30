package com.elorankingbot.model;

import com.elorankingbot.logging.UseToStringForLogging;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.ArrayList;
import java.util.List;

@Data
@UseToStringForLogging
public class MatchFinderQueue {

	public static int NO_LIMIT = -1;

	public enum QueueType {
		SOLO,
		MIXED,
		PREMADE
	}

	@Id
	private String name;
	private List<Group> groups;
	private List<Group> formingGroups;// TODO was ist es und kann es weg? vllt was mit team q?
	@DBRef(lazy = true)
	private Game game;
	private final int numTeams;
	private final int numPlayersPerTeam;
	private final QueueType queueType;
	private final int maxPremadeSize;
	private int maxRatingSpread;
	private int ratingElasticity;
	private int k;
	private boolean isBuildMatchFromTopPlayer;

	// TODO warum funktioniert das mit der db? dieser konstruktor kann es ja wohl kaum sein...?
	// vllt einfach umbauen auf @NoArgsConstructor, rein damit ich verstehe warum es funktioniert.
	public MatchFinderQueue(Game game, String name, int numTeams, int numPlayersPerTeam,
							QueueType queueType, int maxPremadeSize) {
		this.game = game;
		this.name = name;
		this.numTeams = numTeams;
		this.numPlayersPerTeam = numPlayersPerTeam;
		this.queueType = queueType;
		this.maxPremadeSize = maxPremadeSize;
		this.maxRatingSpread = NO_LIMIT;
		this.ratingElasticity = 100;
		this.k = 16;
		this.groups = new ArrayList<>();
		this.formingGroups = new ArrayList<>();
		this.isBuildMatchFromTopPlayer = true;
	}

	public void addGroup(Group group) {
		groups.add(group);
	}

	public void removeGroupsContainingPlayer(Player player) {
		groups.removeIf(group -> group.hasPlayer(player));
	}

	public int getNumPlayersWaiting() {
		return groups.stream().map(group -> group.getPlayers().size()).reduce(0, Integer::sum);
	}

	public boolean updatePlayerIfPresent(Player player) {
		boolean hasQueueChanged = false;
		for (Group group : groups) {
			boolean hasGroupChanged = group.updatePlayerIfPresent(player);
			if (hasGroupChanged) hasQueueChanged = true;
		}
		return hasQueueChanged;
	}

	public int getNumPlayersPerMatch() {
		return numTeams * numPlayersPerTeam;
	}

	public boolean hasPlayer(Player player) {
		return getGroups().stream().anyMatch(group -> group.hasPlayer(player));
	}

	public String getFullName() {
		if (game.getQueues().size() == 1) {
			return game.getName();
		} else {
			return game.getName() + " " + name;
		}
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

	public String getMaxRatingSpreadAsString() {
		return maxRatingSpread == NO_LIMIT ? "no limit" : String.valueOf(maxRatingSpread);
	}

	public Server getServer() {
		return getGame().getServer();
	}
}
