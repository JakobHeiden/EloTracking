package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.UserInteractionEvent;

record UserInteractionEventWrapper (
		UserInteractionEvent event,
		EloTrackingService service,
		DiscordBotService bot,
		TimedTaskQueue queue,
		GatewayDiscordClient client)
{}
