package com.elorankingbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;
import java.util.stream.Collectors;

import static com.elorankingbot.model.Match.ReportIntegrity.INCOMPLETE;
import static com.elorankingbot.model.ReportStatus.*;

@CommonsLog
@Data
@AllArgsConstructor(onConstructor=@__({@PersistenceConstructor}))
@Document(collection = "match")
public class Match {

	public enum ReportIntegrity {
		INCOMPLETE,
		COMPLETE,
		CANCEL,
		CONFLICT
	}

	@Id
	private UUID id;
	@DBRef(lazy = true)
	private Server server;
	private String gameId, queueId;
	private boolean isDispute, hasFirstReport;
	private List<List<Player>> teams;
	private Map<UUID, ReportStatus> playerIdToReportStatus;
	private long messageId, channelId;
	private List<Player> conflictingReports;
	private ReportIntegrity reportIntegrity;// TODO muss nicht persistiert werden, oder?
	private Date timestamp;

	// Match is constructed initially from queue, but persisted with server instead since queue has no collection
	public Match(MatchFinderQueue queue, List<List<Player>> teams) {
		this.id = UUID.randomUUID();
		this.server = queue.getGame().getServer();
		this.gameId = queue.getGame().getName();
		this.queueId = queue.getName();
		this.isDispute = false;
		this.teams = teams;
		this.playerIdToReportStatus = new HashMap<>(queue.getNumPlayersPerMatch());
		for (Player player : this.teams.stream().flatMap(Collection::stream).toList()) {
			this.playerIdToReportStatus.put(player.getId(), NOT_YET_REPORTED);
		}
		this.conflictingReports = new ArrayList<>();
		this.reportIntegrity = INCOMPLETE;
		this.timestamp = new Date();
	}

	public void reportAndSetConflictData(UUID playerId, ReportStatus reportStatus) {
		playerIdToReportStatus.put(playerId, reportStatus);
		setConflictingReports();
		setReportIntegrity();
	}

	// TODO reportIntegrity muss nicht persistiert werden. die ganze logik sollte in getReportIntegrity gekapselt werden.
	private void setConflictingReports() {// TODO refaktorn. TRACE kann dann ueber die function calls automatisch laufen
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
				if (playerReported != NOT_YET_REPORTED) {
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
			log.trace("teamInternalConflicts = " + String.join(",", teamInternalConflicts.stream().map(Player::getTag).toList()));
			return;
		}

		// check for conflicts involving draws and non-draws present at the same time; same for cancel
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
			log.trace("conflict involving draws or cancels = " + String.join(",",
					conflictingReports.stream().map(Player::getTag).toList()));
			return;
		}
		if (!playersReportedDraw.isEmpty() || !playersReportedCancel.isEmpty()) return;

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
		// check for all reports in but no team reporting win
		boolean allReportsIn = playerIdToReportStatus.values().stream().noneMatch(reportStatus -> reportStatus == NOT_YET_REPORTED);
		if (allReportsIn && numberOfTeamsReportingWin == 0) {
			conflictingReports = getPlayers();
		}
		log.trace(conflictingReports.isEmpty() ? "conflictingReports isEmpty"
				: "conflictingReports = " + String.join(",", conflictingReports.stream().map(Player::getTag).toList()));
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
			} else if (playerIdToReportStatus.values().stream().findAny().get() == CANCEL) {
				reportIntegrity = ReportIntegrity.CANCEL;
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

	public boolean containsPlayer(UUID playerId) {
		return playerIdToReportStatus.containsKey(playerId);
	}

	public boolean updatePlayerIfPresent(Player player) {
		boolean hasMatchChanged = false;
		for (List<Player> team : teams) {
			if (team.contains(player)) {
				team.set(team.indexOf(player), player);
				hasMatchChanged = true;
			}
		}
		return hasMatchChanged;
	}

	public ReportStatus getReportStatus(UUID playerId) {
		return playerIdToReportStatus.get(playerId);
	}

	public boolean hasReports() {
		return playerIdToReportStatus.values().stream().anyMatch(reportStatus -> reportStatus != NOT_YET_REPORTED);
	}

	public Game getGame() {
		return server.getGame(gameId);
	}

	public MatchFinderQueue getQueue() {
		return server.getGame(gameId).getQueue(queueId);
	}

	public String getAllMentions() {
		return String.join(" ", getPlayers().stream()
				.map(player -> String.format("<@%s>", player.getUserId())).toList());
	}
}
