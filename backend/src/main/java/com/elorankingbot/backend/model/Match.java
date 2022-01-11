package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor// TODO kann das weg?
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "match")
public class Match {
    @Id
    private UUID id;
    private Date date;
    private long guildId;
    private long winnerId;
    private double winnerOldRating;
    private double winnerNewRating;
    private long loserId;
    private double loserOldRating;
    private double loserNewRating;
    private boolean isDraw;

    public Match(long guildId, long winnerId, long loserId, boolean isDraw) {
        this.guildId = guildId;
        this.winnerId = winnerId;
        this.loserId = loserId;
        this.isDraw = isDraw;
        this.id = UUID.randomUUID();
        this.date = new Date();
    }

    public String getWinnerTag(GatewayDiscordClient client) {// TODO umziehen nach bot
        return client.getUserById(Snowflake.of(winnerId)).block().getTag();
    }

    public String getLoserTag(GatewayDiscordClient client) {
        return client.getUserById(Snowflake.of(loserId)).block().getTag();
    }
}
