package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Game {//TODO add mongodb annotations
    private String channel;
    private String name;
}
