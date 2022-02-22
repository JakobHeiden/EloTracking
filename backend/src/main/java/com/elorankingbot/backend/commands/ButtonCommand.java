package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class ButtonCommand {

	protected final ButtonInteractionEvent event;
	protected final EloRankingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;

	protected static final List none = new ArrayList<>();

	public ButtonCommand(ButtonInteractionEvent event, Services services) {
		this.event = event;
		this.service = services.service;
		this.bot = services.bot;
		this.queue = services.queue;
		this.client = services.client;
	}

	public abstract void execute();
}
