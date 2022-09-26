package com.elorankingbot.backend.service;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.EventParser;
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
	public final ChannelManager channelManager;
	public final DiscordCommandService discordCommandService;
	public final MatchService matchService;
	public final GatewayDiscordClient client;
	public final TimedTaskQueue timedTaskQueue;
	public final TimedTaskService timedTaskService;
	public final QueueService queueService;
	public final CommandClassScanner commandClassScanner;
	public final EventParser eventParser;

	public Services(ApplicationPropertiesLoader props,
					@Lazy DBService dbService, @Lazy DiscordBotService bot, @Lazy ChannelManager channelManager,
					@Lazy DiscordCommandService discordCommandService,
					@Lazy MatchService matchService, @Lazy GatewayDiscordClient client,
					@Lazy TimedTaskQueue timedTaskQueue, @Lazy TimedTaskService timedTaskService, @Lazy QueueService queueService,
					@Lazy CommandClassScanner commandClassScanner, @Lazy EventParser eventParser) {
		this.props = props;
		this.dbService = dbService;
		this.bot = bot;
		this.channelManager = channelManager;
		this.discordCommandService = discordCommandService;
		this.matchService = matchService;
		this.client = client;
		this.timedTaskQueue = timedTaskQueue;
		this.timedTaskService = timedTaskService;
		this.queueService = queueService;
		this.commandClassScanner = commandClassScanner;
		this.eventParser = eventParser;
	}
}
