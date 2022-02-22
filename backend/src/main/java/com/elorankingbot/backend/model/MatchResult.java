package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@ToString
@UseToStringForLogging
@Document
public class MatchResult implements Comparable<MatchResult> {

    private UUID id;
    @DBRef(lazy = true)
    private Game game;
    private Date date;
    private List<List<PlayerMatchResult>> matchResults;

    public MatchResult(Game game) {
        this.id = UUID.randomUUID();
        this.game = game;
        this.date = new Date();
    }

    @Override
    public int compareTo(MatchResult other) {// TODO wird das hier ueberhaupt gebraucht?
        return date.compareTo(other.date);
    }
}
