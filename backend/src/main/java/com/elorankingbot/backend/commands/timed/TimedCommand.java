package com.elorankingbot.backend.commands.timed;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;

import java.util.ArrayList;
import java.util.List;

public abstract class TimedCommand {

	protected final EloRankingService service;
	protected final DiscordBotService bot;
	protected final GatewayDiscordClient client;
	protected final TimedTaskQueue queue;

	protected final long relationId;
	protected final int time;

	protected static final List none = new ArrayList<>();

	public TimedCommand(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client,
						TimedTaskQueue queue, long relationId, int time) {
		this.service = service;
		this.bot = bot;
		this.client = client;
		this.queue = queue;
		this.relationId = relationId;
		this.time = time;
	}

	public abstract void execute();
}
