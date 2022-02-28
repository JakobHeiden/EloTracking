package com.elorankingbot.backend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Document
public class Match {

	public enum ReportStatus {
		NOT_YET_REPORTED,
		WIN,
		LOSE,
		DRAW,
		CANCEL
	}

	public enum ReportIntegrity {
		INCOMPLETE,
		COMPLETE,
		CONFLICT
	}

	private UUID id;
	@DBRef(lazy = true)
	private MatchFinderQueue queue;
	private List<List<Player>> groups;
	private Map<UUID, ReportStatus> playerIdToReportStatus;

	public Match(MatchFinderQueue queue, List<List<Player>> groups) {
		this.id = UUID.randomUUID();
		this.queue = queue;
		this.groups = groups;
		this.playerIdToReportStatus = new HashMap<>();
	}

	public void report(UUID playerId, ReportStatus reportStatus) {
		playerIdToReportStatus.put(playerId, reportStatus);
	}

	public List<Player> getPlayers() {
		return getGroups().stream().flatMap(groups -> groups.stream()).collect(Collectors.toList());
	}
}
