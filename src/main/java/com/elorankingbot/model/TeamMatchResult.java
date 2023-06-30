package com.elorankingbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class TeamMatchResult implements Comparable<TeamMatchResult> {

	private List<PlayerMatchResult> playerMatchResults;

	public TeamMatchResult() {
		playerMatchResults = new ArrayList<>();
	}

	@Override
	public int compareTo(TeamMatchResult other) {
		Double thisValue = this.playerMatchResults.get(0).getResultStatus().value;
		Double otherValue = other.playerMatchResults.get(0).getResultStatus().value;
		return thisValue.compareTo(otherValue);
	}

	public List<Player> getPlayers() {
		return playerMatchResults.stream()
				.map(playerMatchResult -> playerMatchResult.getPlayer())
				.collect(Collectors.toList());
	}

	public void add(PlayerMatchResult playerMatchResult) {
		playerMatchResults.add(playerMatchResult);
	}

	public Stream<PlayerMatchResult> stream() {
		return playerMatchResults.stream();
	}

	public ReportStatus getResultStatus() {
		return playerMatchResults.get(0).getResultStatus();
	}
}
