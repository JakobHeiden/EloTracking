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
    private long guildId;
    private long adminRoleId;
    private long modRoleId;
    private String name;
    private long resultChannelId;
    private long disputeChannelId;
    private boolean allowDraw;
    private int openChallengeDecayTime;
    private int acceptedChallengeDecayTime;
    private int matchAutoResolveTime;
    private int matchSummarizeTime;
    private int deleteMessageTime;

    public Game(long guildId, String name) {
        this.guildId = guildId;
        this.name = name;
        this.openChallengeDecayTime = 2 * 60;
        this.acceptedChallengeDecayTime = 7 * 24 * 60;
        this.matchAutoResolveTime = 24 * 60;
        this.matchSummarizeTime = 24 * 60;
        this.deleteMessageTime = 60;
    }
}
