package com.elorankingbot.backend.commands.timed;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;

import java.util.ArrayList;
import java.util.List;

public abstract class TimedCommand {

	protected final DBService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;

	protected final ChallengeModel challenge;
	protected final long relationId;
	protected final int time;

	protected static final List none = new ArrayList<>();

	public TimedCommand(Services services, long relationId, int time) {
		this.service = services.dbService;
		this.bot = services.bot;
		this.queue = services.queue;
		this.client = services.client;

		this.relationId = relationId;
		this.time = time;
		//Optional<ChallengeModel> maybeChallenge = service.findChallengeById(relationId);
		//if (maybeChallenge.isPresent()) challenge = maybeChallenge.get();
		//else
		challenge = null;
	}

	public abstract void execute();
}
