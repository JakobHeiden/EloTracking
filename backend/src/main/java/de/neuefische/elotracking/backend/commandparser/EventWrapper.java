package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.Event;
import lombok.Getter;

public class EventWrapper {

	@Getter
	private final Event event;
	@Getter
	private final EloTrackingService service;
	@Getter
	private final DiscordBotService bot;
	@Getter
	private final TimedTaskQueue queue;

	public EventWrapper(Event event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
	}
}
