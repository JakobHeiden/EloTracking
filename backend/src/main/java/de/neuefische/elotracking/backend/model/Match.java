package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Match {//TODO annotations for mongoDB
    private String id;
    private String channel;
    private Date date;
    private String winner;
    private String loser;
    private boolean isDraw;
}
