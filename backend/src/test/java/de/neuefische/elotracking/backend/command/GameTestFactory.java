package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.Game;

import java.util.Optional;

class GameTestFactory {

    private static final String NAME = "TestGame";

    public static Optional<Game> create() {
        return Optional.of(new Game(SnowflakeTestFactory.CHANNELID, NAME));
    }
}
