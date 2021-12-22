package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;

@UseToStringForLogging
public
record ApplicationCommandInteractionEventWrapper(
		ApplicationCommandInteractionEvent event,
		EloTrackingService service,
		DiscordBotService bot,
		TimedTaskQueue queue,
		GatewayDiscordClient client)
{
	public String toString() {
		return event.getCommandName();
	}
}