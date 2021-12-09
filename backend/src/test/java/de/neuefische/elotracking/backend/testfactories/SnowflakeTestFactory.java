package de.neuefische.elotracking.backend.testfactories;

import discord4j.common.util.Snowflake;

import java.util.concurrent.ThreadLocalRandom;

public class SnowflakeTestFactory {

    // these cannot be random since @ValueSource needs these to be "a constant expression" - likely a reflection issue
    public static final String CHANNEL_ID = "1";
    public static final String CHALLENGER_ID = "2";
    public static final String ACCEPTOR_ID = "3";
    public static final String ADMIN_ID = "4";

    public static final Snowflake CHANNEL = Snowflake.of(CHANNEL_ID);
    public static final Snowflake CHALLENGER = Snowflake.of(CHALLENGER_ID);
    public static final Snowflake ACCEPTOR = Snowflake.of(ACCEPTOR_ID);
    public static final Snowflake ADMIN = Snowflake.of(ADMIN_ID);

    public static String createId() {
        // Actually Discord IDs are 64 bits unsigned int but this should suffice
        return String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
    }

    public static Snowflake createSnowflake() {
        return Snowflake.of(createId());
    }
}
