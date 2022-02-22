package com.elorankingbot.backend.service;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.timedtask.TimedTaskService;
import discord4j.core.GatewayDiscordClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class Services {

	private final ApplicationPropertiesLoader props;
	private final EloRankingService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final TimedTaskQueue queue;
	private final TimedTaskService timedTaskService;

	public Services(ApplicationPropertiesLoader props,
					@Lazy EloRankingService service, @Lazy DiscordBotService bot, @Lazy GatewayDiscordClient client,
					@Lazy TimedTaskQueue queue, @Lazy TimedTaskService timedTaskService) {
		this.props = props;
		this.service = service;
		this.bot = bot;
		this.client = client;
		this.queue = queue;
		this.timedTaskService = timedTaskService;
	}

	public ApplicationPropertiesLoader props() {
		return props;
	}

	public EloRankingService service() {
		return service;
	}

	public DiscordBotService bot() {
		return bot;
	}

	public GatewayDiscordClient client() {
		return client;
	}

	public TimedTaskQueue queue() {
		return queue;
	}

	public TimedTaskService timedTaskService() {
		return timedTaskService;
	}
}
