package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "match")
public class Match {
    @Id
    private UUID id;
    private String channel;
    private Date date;
    private String winner;
    private String loser;
    private boolean isDraw;
}
