package de.neuefische.elotracking.backend.command;

import discord4j.common.util.Snowflake;

import java.util.concurrent.ThreadLocalRandom;

public class SnowflakeTestFactory {

    // Actually Discord IDs are 64 bits unsigned int but this should suffice
    private static final int biggestIntInJava = (int) java.lang.Math.pow(2, 31) - 1;

    public static Snowflake createRandomSnowflake() {
        int randomNum = ThreadLocalRandom.current().nextInt(0, biggestIntInJava + 1);
        return Snowflake.of(String.valueOf(randomNum));
    }
}
