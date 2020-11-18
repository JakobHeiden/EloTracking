package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {//TODO add mongoDB annotations
    private String id;
    private String channel;
    private double rating;
}
