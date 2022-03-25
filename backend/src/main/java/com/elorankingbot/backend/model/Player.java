package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "player")
public class Player  {

    private static final int PERMABANNED = -1;
    private static final int NOT_BANNED = -2;

    @Id
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

    public PlayerGameStats getGameStats(Game game) {
        PlayerGameStats playerGameStats = gameNameToPlayerGameStats.get(game.getName());
        if (playerGameStats == null) {
            PlayerGameStats newPlayerGameStats = new PlayerGameStats(1200);
            gameNameToPlayerGameStats.put(game.getName(), newPlayerGameStats);
            playerGameStats = newPlayerGameStats;
        }
        return playerGameStats;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Player)) return false;
        return this.id.equals(((Player) other).id);
    }
}
