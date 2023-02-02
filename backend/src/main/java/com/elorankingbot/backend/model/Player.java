package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@UseToStringForLogging
@Document(collection = "player")
public class Player  {

    private static final int PERMABANNED = -1;
    private static final int NOT_BANNED = -2;

    @Id
    @EqualsAndHashCode.Include
    private UUID id;
    private long userId;
    private long guildId;
    private String tag;
    private int unbanAtTimeSlot;
    private Map<String, PlayerGameStats> gameNameToPlayerGameStats;

    public Player(long guildId, long userId, String tag) {
        this.id = generateId(guildId, userId);
        this.userId = userId;
        this.guildId = guildId;
        this.tag = tag;
        this.unbanAtTimeSlot = NOT_BANNED;
        this.gameNameToPlayerGameStats = new HashMap<>();
    }

    public static UUID generateId(long guildId, long userId) {
        return new UUID(guildId, userId);
    }

    public boolean isBanned() {
        return unbanAtTimeSlot != NOT_BANNED;
    }

    public boolean isPermaBanned() {
        return unbanAtTimeSlot == PERMABANNED;
    }

    public PlayerGameStats getOrCreatePlayerGameStats(Game game) {
        PlayerGameStats playerGameStats = gameNameToPlayerGameStats.get(game.getName());
        if (playerGameStats == null) {
            PlayerGameStats newPlayerGameStats = new PlayerGameStats(game.getInitialRating());
            gameNameToPlayerGameStats.put(game.getName(), newPlayerGameStats);
            playerGameStats = newPlayerGameStats;
        }
        return playerGameStats;
    }

    public boolean hasPlayerGameStats(Game game) {
        PlayerGameStats playerGameStats = gameNameToPlayerGameStats.get(game.getName());
        return playerGameStats != null;
    }

    public void deleteGameStats(Game game) {
        gameNameToPlayerGameStats.remove(game.getName());
    }

    public void addMatchResult(MatchResult matchResult) {
        PlayerMatchResult playerMatchResult = matchResult.getPlayerMatchResult(id);
        PlayerGameStats playerGameStats = gameNameToPlayerGameStats.get(matchResult.getGame().getName());
        playerGameStats.setRating(playerMatchResult.getNewRating());
        playerGameStats.getMatchHistory().add(matchResult.getId());
        playerGameStats.addResultStatus(matchResult.getResultStatus(this));
    }
}
