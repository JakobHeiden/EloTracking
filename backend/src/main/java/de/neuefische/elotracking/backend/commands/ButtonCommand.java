package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class ButtonCommand {

	protected final EloTrackingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;
	protected final ButtonInteractionEvent event;

	protected final List none = new ArrayList<>();

	protected ButtonCommand(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
							GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;
	}

	public abstract void execute();
}
