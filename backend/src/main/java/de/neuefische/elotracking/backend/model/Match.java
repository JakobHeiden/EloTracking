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
    private long guildId;
    private long winnerId;
    private double winnerBeforeRating;
    private double winnerAfterRating;
    private long loserId;
    private double loserBeforeRating;
    private double loserAfterRating;
    private boolean isDraw;

    public Match(long guildId, long winnerId, long loserId, boolean isDraw) {
        this.guildId = guildId;
        this.winnerId = winnerId;
        this.loserId = loserId;
        this.isDraw = isDraw;
        this.id = UUID.randomUUID();
        this.date = new Date();
    }
}
