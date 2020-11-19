package de.neuefische.elotracking.backend.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tinylog.Logger;

@Configuration
public class DiscordBotConfig {
    @Bean
    public GatewayDiscordClient createClient() {
        GatewayDiscordClient client = DiscordClientBuilder
                .create(System.getenv("DISCORD_BOT_TOKEN"))
                .build()
                .login()
                .block();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    Logger.info("Logged in as {}#{}", self.getUsername(), self.getDiscriminator());
                });

        return client;
    }
}
