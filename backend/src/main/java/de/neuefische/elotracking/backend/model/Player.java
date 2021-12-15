package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
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

    public Player(long guildId, long userId, double rating) {
        this.id = generateId(guildId, userId);
        this.userId = userId;
        this.guildId = guildId;
        this.rating = rating;
    }

    public static UUID generateId(long guildId, long userId) {
        return new UUID(guildId, userId);
    }
}
