package com.elorankingbot.backend.configuration;

import com.elorankingbot.backend.service.EloRankingService;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayDiscordClientConfiguration {

	@Bean
	public GatewayDiscordClient createClient(EloRankingService service) {
		String botToken = service.getPropertiesLoader().isUseDevBotToken() ?
				System.getenv("DEV_BOT_TOKEN")
				: System.getenv("DISCORD_BOT_TOKEN");
		GatewayDiscordClient client = DiscordClientBuilder
				.create(botToken)
				.build()
				.login()
				.block();

		client.getEventDispatcher().on(ReadyEvent.class)
				.subscribe(event -> {
					User self = event.getSelf();
					log.info("Logged in as {}#{}", self.getUsername(), self.getDiscriminator());
				});

		return client;
	}

}
