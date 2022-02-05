package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "player")
public class Player implements Comparable<Player> {

    @Id
    private UUID id;
    private long userId;
    private long guildId;
    private String name;
    private double rating;
    private int wins;
    private int draws;
    private int losses;
    private int unbanAtTimeSlot;

    public Player(long guildId, long userId, String name, double rating) {
        this.id = generateId(guildId, userId);
        this.userId = userId;
        this.guildId = guildId;
        this.name = name;
        this.rating = rating;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.unbanAtTimeSlot = -2;// not banned
    }

    public void addWin() {
        wins++;
    }

    public void addDraw() {
        draws++;
    }

    public void addLoss() {
        losses++;
    }

    public static UUID generateId(long guildId, long userId) {
        return new UUID(guildId, userId);
    }

    public boolean isBanned() {
        return unbanAtTimeSlot != -2;
    }

    public boolean isPermaBanned() {
        return unbanAtTimeSlot == -1;
    }

    @Override
    public int compareTo(Player otherPlayer) {
        return Double.compare(this.rating, otherPlayer.rating);
    }
}
