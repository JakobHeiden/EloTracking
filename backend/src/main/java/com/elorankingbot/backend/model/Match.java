package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "match")
public class Match implements Comparable<Match> {

    @Id
    private UUID id;
    private UUID gameId;
    private Date date;
    private List<PlayerMatchResult> playerMatchResults;
    private boolean isDraw;

    public Match(UUID gameId, boolean isDraw) {
        this.id = UUID.randomUUID();
        this.gameId = gameId;
        this.date = new Date();
        this.isDraw = isDraw;
    }

    @Override
    public int compareTo(Match other) {
        return date.compareTo(other.date);
    }
}
