package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.List;

public abstract class SlashCommand {

	protected final DBService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue timedTaskQueue;
	protected final GatewayDiscordClient client;
	protected final ChatInputInteractionEvent event;
	protected final long guildId;
	protected final Server server;
	protected final User activeUser;

	protected static final List none = new ArrayList<>();

	protected SlashCommand(ChatInputInteractionEvent event, Services services) {
		this.event = event;
		this.service = services.service;
		this.bot = services.bot;
		this.timedTaskQueue = services.queue;
		this.client = services.client;

		this.guildId = event.getInteraction().getGuildId().get().asLong();
		this.server = service.findServerByGuildId(guildId).get();
		this.activeUser = event.getInteraction().getUser();
	}

	public abstract void execute();
}
