package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
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
    private String channel;
    private String winner;
    private String loser;
    private boolean isDraw;

    public Match(String channel, String winner, String loser, boolean isDraw) {
        this.channel = channel;
        this.winner = winner;
        this.loser = loser;
        this.isDraw = isDraw;
        this.id = UUID.randomUUID();
        this.date = new Date();
    }
}
