package de.neuefische.elotracking.backend.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {

    @Bean
    public GatewayDiscordClient createClient() throws Exception {
        GatewayDiscordClient client = DiscordClientBuilder
                .create(System.getenv("DISCORD_BOT_TOKEN"))
                .build()
                .login()
                .block();
        client.onDisconnect().block();
        System.err.println("config");
        throw new Exception("oh no");
        //return client;
    }
}
