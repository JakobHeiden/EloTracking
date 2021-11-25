package de.neuefische.elotracking.backend.commands;

import discord4j.common.util.Snowflake;

import java.util.concurrent.ThreadLocalRandom;

class SnowflakeTestFactory {

    static final String CHANNEL_ID = "1";
    static final String CHALLENGER_ID = "2";
    static final String ACCEPTOR_ID = "3";

    static final Snowflake CHANNEL = Snowflake.of(CHANNEL_ID);
    static final Snowflake CHALLENGER = Snowflake.of(CHALLENGER_ID);
    static final Snowflake ACCEPTOR = Snowflake.of(ACCEPTOR_ID);

    public static String createId() {
        // Actually Discord IDs are 64 bits unsigned int but this should suffice
        return String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
    }

    public static Snowflake createSnowflake() {
        return Snowflake.of(createId());
    }
}
