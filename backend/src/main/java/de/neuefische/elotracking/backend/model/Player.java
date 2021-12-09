package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "player")
public class Player {
    @Id
    private String id;
    private String discordUserId;
    private String channelId;
    private double rating;

    public Player(String channelId, String discordUserId, double rating) {
        this.id = generateId(channelId, discordUserId);
        this.discordUserId = discordUserId;
        this.channelId = channelId;
        this.rating = rating;
    }

    public static String generateId(String channelId, String discordUserId) {
        return String.format("%s-%s", channelId, discordUserId);
    }
}
