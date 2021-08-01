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
@Document(collection = "game")
public class Game {
    @Id
    private String channelId;
    private String name;
    private String commandPrefix;

    public Game(String channelId, String name) {
        this(channelId, name, "!");//TODO
    }
}
