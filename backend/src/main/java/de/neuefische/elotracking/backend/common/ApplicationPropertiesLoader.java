package de.neuefische.elotracking.backend.common;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationPropertiesLoader {
    @Value("${admin.discord.id}")
    private String ADMIN_DISCORD_ID;
    @Value("${default.command.prefix}")
    private String DEFAULT_COMMAND_PREFIX;
    @Value("${k}")
    private String K;
    @Value("${initial.rating}")
    private String INITIAL_RATING;
    @Value("${base.url}")
    private String BASE_URL;

    public String getProperty(String name) {
        switch (name) {
            case "ADMIN_DISCORD_ID":
                return ADMIN_DISCORD_ID;
            case "DEFAULT_COMMAND_PREFIX":
                return DEFAULT_COMMAND_PREFIX;
            case "K":
                return K;
            case "INITIAL_RATING":
                return INITIAL_RATING;
            case "BASE_URL":
                return BASE_URL;
            default:
                return null;
        }
    }
}
