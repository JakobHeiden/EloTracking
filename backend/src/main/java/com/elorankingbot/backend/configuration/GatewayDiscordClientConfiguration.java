package com.elorankingbot.backend.configuration;

import com.elorankingbot.backend.service.Services;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import io.netty.channel.unix.Errors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.retry.Retry;

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
				.onClientResponse(ResponseFunction.retryWhen(
						RouteMatcher.any(),
						Retry.anyOf(Errors.NativeIoException.class)))
				.build()
				.login()
				.block();

		return client;
	}
}
