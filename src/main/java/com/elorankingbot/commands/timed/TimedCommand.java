package com.elorankingbot.commands.timed;

import com.elorankingbot.service.DBService;
import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.Services;
import com.elorankingbot.timedtask.TimedTaskScheduler;
import discord4j.core.GatewayDiscordClient;

import java.util.ArrayList;
import java.util.List;

public abstract class TimedCommand {

	protected final DBService service;
	protected final DiscordBotService bot;
	protected final TimedTaskScheduler queue;
	protected final GatewayDiscordClient client;

	protected final long relationId;
	protected final int time;

	protected static final List none = new ArrayList<>();

	public TimedCommand(Services services, long relationId, int time) {
		this.service = services.dbService;
		this.bot = services.bot;
		this.queue = services.timedTaskScheduler;
		this.client = services.client;

		this.relationId = relationId;
		this.time = time;
	}

	public abstract void execute();
}
