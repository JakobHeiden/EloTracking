package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "match")
@CompoundIndex(def = "{'guildId': 1, 'winnerId': 1, 'date': -1}")
@CompoundIndex(def = "{'guildId': 1, 'loserId': 1, 'date': -1}")
public class Match implements Comparable<Match> {

    @Id
    private UUID id;
    private Date date;
    private long guildId;
    private long winnerId;
    private String winnerTag;
    private double winnerOldRating;
    private double winnerNewRating;
    private long loserId;
    private String loserTag;
    private double loserOldRating;
    private double loserNewRating;
    private boolean isDraw;

    public Match(long guildId, long winnerId, long loserId, String winnerTag, String loserTag, boolean isDraw) {
        this.guildId = guildId;
        this.winnerId = winnerId;
        this.loserId = loserId;
        this.winnerTag = winnerTag;
        this.loserTag = loserTag;
        this.isDraw = isDraw;
        this.id = UUID.randomUUID();
        this.date = new Date();
    }

    public String getWinnerTag(GatewayDiscordClient client) {// TODO umziehen nach bot, oder mit persistieren?
        return client.getUserById(Snowflake.of(winnerId)).block().getTag();
    }

    @Override
    public int compareTo(Match other) {
        return date.compareTo(other.date);
    }
}
