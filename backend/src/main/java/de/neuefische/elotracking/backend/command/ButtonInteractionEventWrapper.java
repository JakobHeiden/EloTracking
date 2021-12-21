package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

record ButtonInteractionEventWrapper(
		ButtonInteractionEvent event,
		EloTrackingService service,
		DiscordBotService bot,
		TimedTaskQueue queue)
{}
