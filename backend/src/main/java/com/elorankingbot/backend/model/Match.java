package com.elorankingbot.backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;
import java.util.stream.Collectors;

import static com.elorankingbot.backend.model.Match.ReportIntegrity.CONFLICT;
import static com.elorankingbot.backend.model.Match.ReportIntegrity.INCOMPLETE;
import static com.elorankingbot.backend.model.ReportStatus.*;

@Getter
@Document(collection = "match")
public class Match {

	public enum ReportIntegrity {
		INCOMPLETE,
		COMPLETE,
		CONFLICT
	}

	@Id
	private UUID id;
	@DBRef(lazy = true)
	private final Server server;
	private final String gameId, queueId;
	@Setter
	@Getter
	private boolean isDispute;
	@Getter
	@Setter
	private boolean isOrWasConflict;
	private final List<List<Player>> teams;
	private final Map<UUID, ReportStatus> playerIdToReportStatus;
	private final Map<UUID, Long> playerIdToMessageId, playerIdToPrivateChannelId;
	private List<Player> conflictingReports;
	private ReportIntegrity reportIntegrity;

	// Match is constructed initially from queue, but persisted with server instead since queue has no collection
	public Match(MatchFinderQueue queue, List<List<Player>> teams) {
		this.id = UUID.randomUUID();
		this.server = queue.getGame().getServer();
		this.gameId = queue.getGame().getName();
		this.queueId = queue.getName();
		this.isDispute = false;
		this.isOrWasConflict = false;
		this.teams = teams;
		this.playerIdToReportStatus = new HashMap<>(queue.getNumPlayers());
		for (Player player : this.teams.stream().flatMap(Collection::stream).toList()) {
			this.playerIdToReportStatus.put(player.getId(), NOT_YET_REPORTED);
		}
		this.playerIdToMessageId = new HashMap<>(queue.getNumPlayers());
		this.playerIdToPrivateChannelId = new HashMap<>(queue.getNumPlayers());
		this.conflictingReports = new ArrayList<>();
		this.reportIntegrity = INCOMPLETE;
	}

	@PersistenceConstructor
	public Match(UUID id, Server server, String gameId, String queueId, boolean isDispute, boolean isOrWasConflict,
				 List<List<Player>> teams, Map<UUID, ReportStatus> playerIdToReportStatus, Map<UUID, Long> playerIdToMessageId,
				 Map<UUID, Long> playerIdToPrivateChannelId, List<Player> conflictingReports, ReportIntegrity reportIntegrity) {
		this.id = id;
		this.server = server;
		this.gameId = gameId;
		this.queueId = queueId;
		this.isDispute = isDispute;
		this.isOrWasConflict = isOrWasConflict;
		this.teams = teams;
		this.playerIdToReportStatus = playerIdToReportStatus;
		this.playerIdToMessageId = playerIdToMessageId;
		this.playerIdToPrivateChannelId = playerIdToPrivateChannelId;
		this.conflictingReports = conflictingReports;
		this.reportIntegrity = reportIntegrity;
	}

	public void reportAndSetConflictData(UUID playerId, ReportStatus reportStatus) {
		playerIdToReportStatus.put(playerId, reportStatus);
		setConflictingReports();
		setReportIntegrity();
		isOrWasConflict = isOrWasConflict || reportIntegrity == CONFLICT;
	}

	private void setConflictingReports() {
		MatchFinderQueue queue = server.getGame(gameId).getQueue(queueId);
		conflictingReports = new ArrayList<>();
		List<ReportStatus> teamReports = new ArrayList<>(queue.getNumTeams());

		// check for team internal conflicts
		List<Player> teamInternalConflicts = new ArrayList<>(queue.getNumPlayersPerTeam());
		for (List<Player> team : teams) {
			ReportStatus teamReported = null;
			for (Player player : team) {
				boolean teamInternalConflict = false;
				ReportStatus playerReported = playerIdToReportStatus.get(player.getId());
				if (playerReported != null) {
					if (teamReported == null) {
						teamReported = playerReported;
					} else {
						if (playerReported != teamReported) {
							teamInternalConflict = true;
						}
					}
				}
				if (teamInternalConflict) {
					teamInternalConflicts.addAll(team);
				}
			}
			teamReports.add(teamReported);
		}
		if (teamInternalConflicts.size() > 0) {
			conflictingReports = teamInternalConflicts;
			return;
		}

		// check for conflicts involving draws and non-draws
		List<Player> playersReportedDraw = new ArrayList<>(getNumPlayers());
		List<Player> playersReportedCancel = new ArrayList<>(getNumPlayers());
		List<Player> playersReportedWinOrLoss = new ArrayList<>(getNumPlayers());
		for (Player player : getPlayers()) {
			ReportStatus reportStatus = playerIdToReportStatus.get(player.getId());
			if (reportStatus == DRAW) playersReportedDraw.add(player);
			if (reportStatus == CANCEL) playersReportedCancel.add(player);
			if (reportStatus == WIN || reportStatus == LOSE) playersReportedWinOrLoss.add(player);
		}
		if ((playersReportedDraw.size() > 0 && playersReportedCancel.size() > 0)
				|| (playersReportedDraw.size() > 0 && playersReportedWinOrLoss.size() > 0)
				|| (playersReportedCancel.size() > 0 && playersReportedWinOrLoss.size() > 0)) {
			// Only mark reports coming from a minority of players
			if (playersReportedDraw.size() <= getNumPlayers() / 2) {
				conflictingReports.addAll(playersReportedDraw);
			}
			if (playersReportedCancel.size() <= getNumPlayers() / 2) {
				conflictingReports.addAll(playersReportedCancel);
			}
			if (playersReportedWinOrLoss.size() <= getNumPlayers() / 2) {
				conflictingReports.addAll(playersReportedWinOrLoss);
			}
			return;
		}

		// check for conflicts with more than one team reporting win
		int numberOfTeamsReportingWin = 0;
		for (ReportStatus reportStatus : teamReports) {
			if (reportStatus == WIN) numberOfTeamsReportingWin++;
		}
		if (numberOfTeamsReportingWin > 1) {
			conflictingReports = getPlayers().stream()
					.filter(player -> playerIdToReportStatus.get(player.getId()) == WIN)
					.collect(Collectors.toList());
		}
	}

	private void setReportIntegrity() {
		if (conflictingReports.size() > 0) {
			reportIntegrity = ReportIntegrity.CONFLICT;
		} else {
			long numPlayersAlreadyReported = playerIdToReportStatus.values().stream()
					.filter(reportStatus -> !reportStatus.equals(NOT_YET_REPORTED))
					.count();
			if (numPlayersAlreadyReported < getNumPlayers()) {
				reportIntegrity = ReportIntegrity.INCOMPLETE;
			} else {
				reportIntegrity = ReportIntegrity.COMPLETE;
			}
		}
	}

	public int getNumTeams() {
		return teams.size();
	}

	public int getNumPlayers() {// TODO vllt private?
		return getPlayers().size();
	}

	public List<Player> getPlayers() {
		return this.getTeams().stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	public Player getPlayer(long userId) {
		return this.getTeams().stream().flatMap(Collection::stream)
				.filter(player -> player.getUserId() == userId).findAny().get();
	}

	public ReportStatus getReportStatus(UUID playerId) {
		return playerIdToReportStatus.get(playerId);
	}

	public long getMessageId(UUID playerId) {
		return playerIdToMessageId.get(playerId);
	}

	public long getPrivateChannelId(UUID playerId) {
		return playerIdToPrivateChannelId.get(playerId);
	}

	public Game getGame() {
		return server.getGame(gameId);
	}

	public MatchFinderQueue getQueue() {
		return server.getGame(gameId).getQueue(queueId);
	}
}
