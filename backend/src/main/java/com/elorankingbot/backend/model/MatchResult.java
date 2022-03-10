package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@ToString
@UseToStringForLogging
@Document
public class MatchResult implements Comparable<MatchResult> {

    private UUID id;
    @DBRef(lazy = true)
    private Game game;
    private Date date;
    private List<TeamMatchResult> teamMatchResults;

    public MatchResult(Match match) {
        this.id = match.getId();
        this.game = match.getQueue().getGame();
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
        return teamMatchResults.stream().flatMap(TeamMatchResult::stream)
                .filter(playerMatchResult -> playerMatchResult.getPlayer().getId() == playerId).findAny().get();
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

    @Override
    public int compareTo(MatchResult other) {
        return date.compareTo(other.date);
    }
}
