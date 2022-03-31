package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document
public class MatchResult implements Comparable<MatchResult> {

	private UUID id;
	/*
	@DBRef(lazy = true)
	private Game game;
	 */
	@DBRef(lazy = true)
	private Server server;
	private String gameName;
	private Date date;
	private List<TeamMatchResult> teamMatchResults;

	public MatchResult(Match match) {
		this.id = match.getId();
		this.server = match.getServer();
		this.gameName = match.getGame().getName();
		this.date = new Date();
		this.teamMatchResults = new ArrayList<>();
	}

	public void addTeamMatchResult(TeamMatchResult teamMatchResult) {
		teamMatchResults.add(teamMatchResult);
	}

	public List<PlayerMatchResult> getAllPlayerMatchResults() {
		return teamMatchResults.stream().flatMap(TeamMatchResult::stream).collect(Collectors.toList());
	}

	public PlayerMatchResult getPlayerMatchResult(UUID playerId) {
		return teamMatchResults.stream()
				.flatMap(teamMatchResult -> teamMatchResult.getPlayerMatchResults().stream())
				.filter(playerMatchResult -> playerMatchResult.getPlayer().getId().equals(playerId)).findAny().get();
	}

	public ReportStatus getResultStatus(Player player) {
		return teamMatchResults.stream().flatMap(TeamMatchResult::stream)
				.filter(playerMatchResult -> playerMatchResult.getPlayer().equals(player))
				.map(PlayerMatchResult::getResultStatus).findAny().get();
	}

	public List<Player> getPlayers() {
		return teamMatchResults.stream()
				.flatMap(teamMatchResult -> teamMatchResult.getPlayers().stream())
				.collect(Collectors.toList());
	}

	public Game getGame() {
		return server.getGame(gameName);
	}

	@Override
	public int compareTo(MatchResult other) {
		return date.compareTo(other.date);
	}
}
