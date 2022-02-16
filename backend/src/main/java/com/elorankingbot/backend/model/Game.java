package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@NoArgsConstructor
@Data
@ToString
@UseToStringForLogging
public class Game {

    private String name;
    private long guildId;
    private Map<String, MatchFinderModality> matchFindModalities;
    private long resultChannelId;
    private long leaderboardMessageId;
    private int leaderboardLength;

    public Game(long guildId, String name) {
        this.name = name;
        this.guildId = guildId;
        this.matchFindModalities = new HashMap<>();
        this.leaderboardLength = 20;
    }

    public void addMatchFinderModality(MatchFinderModality matchFinderModality) {
        matchFindModalities.put(matchFinderModality.getName(), matchFinderModality);
    }
}
