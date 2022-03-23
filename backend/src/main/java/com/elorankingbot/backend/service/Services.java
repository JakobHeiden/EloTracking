package com.elorankingbot.backend.service;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.timedtask.TimedTaskService;
import discord4j.core.GatewayDiscordClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class Services {

	public final ApplicationPropertiesLoader props;
	public final DBService dbService;
	public final DiscordBotService bot;
	public final MatchService matchService;
	public final GatewayDiscordClient client;
	public final TimedTaskQueue queue;
	public final TimedTaskService timedTaskService;
	public final QueueService queueService;
	public final CommandClassScanner commandClassScanner;

	public Services(ApplicationPropertiesLoader props,
					@Lazy DBService dbService, @Lazy DiscordBotService bot, @Lazy MatchService matchService,
					@Lazy GatewayDiscordClient client,
					@Lazy TimedTaskQueue queue, @Lazy TimedTaskService timedTaskService, @Lazy QueueService queueService,
					@Lazy CommandClassScanner commandClassScanner) {
		this.props = props;
		this.dbService = dbService;
		this.bot = bot;
		this.matchService = matchService;
		this.client = client;
		this.queue = queue;
		this.timedTaskService = timedTaskService;
		this.queueService = queueService;
		this.commandClassScanner = commandClassScanner;
	}
}
