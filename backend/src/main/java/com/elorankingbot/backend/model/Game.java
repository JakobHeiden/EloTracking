package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "game")
public class Ranking {

    private UUID id;
    private long guildId;
    private Set<MatchFinderModality> matchFindModalities;
    private String name;
    private boolean allowDraw;
    private long resultChannelId;
    private long leaderboardMessageId;
    private int leaderboardLength;

    public Ranking(String name, long guildId, boolean allowDraw) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.guildId = guildId;
        this.matchFindModalities = new HashSet<>();
        this.allowDraw = allowDraw;
        this.leaderboardLength = 20;
    }
}
