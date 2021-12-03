package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "game")
public class Game {

    @Id
    private String channelId;
    private String name;
    private String commandPrefix;
    private int openChallengeDecayTime;
    private int acceptedChallengeDecayTime;
    private int matchAutoResolveTime;

    public Game(String channelId, String name) {
        this.channelId = channelId;
        this.name = name;
        this.commandPrefix = "!";
        this.openChallengeDecayTime = 2 * 60;
        this.acceptedChallengeDecayTime = 7 * 24 * 60;
        this.matchAutoResolveTime = 24 * 60;
    }
}
