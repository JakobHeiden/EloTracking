package de.neuefische.elotracking.backend.testfactories;

import de.neuefische.elotracking.backend.model.Game;

import java.util.Optional;

public class GameTestFactory {

    private static final String NAME = "TestGame";

    public static Optional<Game> create() {
        return Optional.of(new Game(SnowflakeTestFactory.CHANNEL_ID, NAME));
    }
}
