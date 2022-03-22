package com.elorankingbot.backend.configuration;

import com.elorankingbot.backend.service.Services;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayDiscordClientConfiguration {

	@Bean
	public GatewayDiscordClient createClient(Services services) {
		String botToken = services.props.isUseDevBotToken() ?
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

					String activityMessage = services.props.getActivityMessage();
					client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(activityMessage))).subscribe();
				});

		return client;

		/* TODO was ist hiermit?
		String activityMessage = services.applicationPropertiesLoader().getActivityMessage();
		GatewayDiscordClient client = GatewayBootstrap.create(DiscordClient.create(token))
				.setInitialPresence(shardInfo -> ClientPresence.of(
						Status.ONLINE, ClientActivity.of(Activity.Type.PLAYING, activityMessage, null)))
				.login().block();
		 */
	}
}
