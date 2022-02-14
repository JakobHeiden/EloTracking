package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
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
    private long resultChannelId;
    private long disputeCategoryId;
    private long leaderboardChannelId;// TODO kann weg?
    private long leaderboardMessageId;
    private String name;
    private boolean allowDraw = false;
    private int openChallengeDecayTime;
    private int acceptedChallengeDecayTime;
    private int matchAutoResolveTime;
    private int messageCleanupTime;
    private int leaderboardLength;
    private boolean isMarkedForDeletion = false;

    public Game(long guildId, String name) {
        this.guildId = guildId;
        this.name = name;
        this.openChallengeDecayTime = 2 * 60;
        this.acceptedChallengeDecayTime = 7 * 24 * 60;
        this.matchAutoResolveTime = 24 * 60;
        this.messageCleanupTime = 12 * 60;
        this.leaderboardLength = 20;
    }
}
