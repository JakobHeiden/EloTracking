package com.elorankingbot.backend.commands.timed;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class TimedCommand {

	protected final EloRankingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;

	protected final ChallengeModel challenge;
	protected final long relationId;
	protected final int time;

	protected static final List none = new ArrayList<>();

	public TimedCommand(EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
						GatewayDiscordClient client, long relationId, int time) {
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;

		this.relationId = relationId;
		this.time = time;
		//Optional<ChallengeModel> maybeChallenge = service.findChallengeById(relationId);
		//if (maybeChallenge.isPresent()) challenge = maybeChallenge.get();
		//else
		challenge = null;
	}

	public abstract void execute();
}
