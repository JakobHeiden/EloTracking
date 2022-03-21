package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.List;

public abstract class SlashCommand {

	protected final DBService dbService;
	protected final DiscordBotService bot;
	protected final GatewayDiscordClient client;
	protected final MatchService matchService;
	protected final QueueService queueService;
	protected final TimedTaskQueue timedTaskQueue;
	protected final ChatInputInteractionEvent event;
	protected final long guildId;
	protected final Server server;
	protected final User activeUser;
	protected final long activeUserId;

	protected static final List none = new ArrayList<>();

	protected SlashCommand(ChatInputInteractionEvent event, Services services) {
		this.event = event;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.matchService = services.matchService;
		this.queueService = services.queueService;
		this.client = services.client;
		this.timedTaskQueue = services.queue;

		this.guildId = event.getInteraction().getGuildId().get().asLong();
		this.server = dbService.findServerByGuildId(guildId).get();
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
	}

	public abstract void execute();
}
