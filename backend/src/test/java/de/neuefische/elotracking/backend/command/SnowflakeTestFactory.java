package de.neuefische.elotracking.backend.command;

import discord4j.common.util.Snowflake;

import java.util.concurrent.ThreadLocalRandom;

class SnowflakeTestFactory {

    static final String CHANNELID = createId();
    static final String CHALLENGERID = createId();
    static final String RECIPIENTID = createId();

    static final Snowflake CHANNEL = Snowflake.of(CHANNELID);
    static final Snowflake CHALLENGER = Snowflake.of(CHALLENGERID);
    static final Snowflake RECIPIENT = Snowflake.of(RECIPIENTID);

    public static String createId() {
        // Actually Discord IDs are 64 bits unsigned int but this should suffice
        return String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
    }

    public static Snowflake createSnowflake() {
        return Snowflake.of(createId());
    }
}
