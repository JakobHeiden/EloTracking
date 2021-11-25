package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;

public class Throw extends Command {

	public Throw(Message msg, EloTrackingService service, DiscordBotService bot) {
		super(msg, service, bot);
	}

	public void execute() {
		throw new RuntimeException("oh no");
	}
}
