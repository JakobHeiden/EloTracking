package com.elorankingbot.service;

import com.elorankingbot.command.CommandClassScanner;
import com.elorankingbot.command.DiscordCommandManager;
import com.elorankingbot.command.EventParser;
import com.elorankingbot.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.logging.ExceptionHandler;
import com.elorankingbot.patreon.PatreonClient;
import com.elorankingbot.timedtask.TimedTaskScheduler;
import com.elorankingbot.timedtask.TimedTaskService;
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
