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
public class Player {

    @Id
    private UUID id;
    private long userId;
    private long guildId;
    private double rating;
    private int unbanAtTimeSlot;

    public Player(long guildId, long userId, double rating) {
        this.id = generateId(guildId, userId);
        this.userId = userId;
        this.guildId = guildId;
        this.rating = rating;
        this.unbanAtTimeSlot = -2;// not banned
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
}
