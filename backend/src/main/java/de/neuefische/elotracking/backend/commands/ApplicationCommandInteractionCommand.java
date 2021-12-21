package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;

// Subclasses must start with a capital letter and have no other capital letters to be recognized by the parser
public abstract class ApplicationCommandInteractionCommand {

	protected EloTrackingService service;
	protected DiscordBotService bot;
	protected TimedTaskQueue queue;
	protected final ApplicationCommandInteractionEvent event;
	protected long guildId;
	protected Game game;

	protected ApplicationCommandInteractionCommand(ApplicationCommandInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.guildId = event.getInteraction().getGuildId().get().asLong();
	}

	public abstract void execute();
}
