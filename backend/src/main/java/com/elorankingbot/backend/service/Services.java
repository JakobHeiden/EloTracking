package com.elorankingbot.backend.service;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.DiscordCommandManager;
import com.elorankingbot.backend.command.EventParser;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.logging.ExceptionHandler;
import com.elorankingbot.backend.patreon.PatreonClient;
import com.elorankingbot.backend.timedtask.TimedTaskScheduler;
import com.elorankingbot.backend.timedtask.TimedTaskService;
import discord4j.core.GatewayDiscordClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class Services {

	public final ExceptionHandler exceptionHandler;
	public final ApplicationPropertiesLoader props;
	public final GatewayDiscordClient client;
	public final PatreonClient patreonClient;
	public final DBService dbService;
	public final DiscordBotService bot;
	public final ChannelManager channelManager;
	public final DiscordCommandManager discordCommandManager;
	public final MatchService matchService;
	public final TimedTaskScheduler timedTaskScheduler;
	public final TimedTaskService timedTaskService;
	public final QueueScheduler queueScheduler;
	public final CommandClassScanner commandClassScanner;
	public final EventParser eventParser;

    public Services(ApplicationPropertiesLoader props,
					@Lazy DBService dbService, @Lazy DiscordBotService bot, @Lazy ChannelManager channelManager,
					@Lazy DiscordCommandManager discordCommandManager,
					@Lazy MatchService matchService, @Lazy GatewayDiscordClient client, @Lazy PatreonClient patreonClient,
					@Lazy TimedTaskScheduler timedTaskScheduler, @Lazy TimedTaskService timedTaskService, @Lazy QueueScheduler queueScheduler,
					@Lazy CommandClassScanner commandClassScanner, @Lazy EventParser eventParser, @Lazy ExceptionHandler exceptionHandler) {
		this.props = props;
		this.dbService = dbService;
		this.bot = bot;
		this.channelManager = channelManager;
		this.discordCommandManager = discordCommandManager;
		this.matchService = matchService;
		this.client = client;
		this.patreonClient = patreonClient;
		this.timedTaskScheduler = timedTaskScheduler;
		this.timedTaskService = timedTaskService;
		this.queueScheduler = queueScheduler;
		this.commandClassScanner = commandClassScanner;
		this.eventParser = eventParser;
		this.exceptionHandler = exceptionHandler;
	}
}
