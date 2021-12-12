package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;

public record EventWrapper(
		ChatInputInteractionEvent event,
		EloTrackingService service,
		DiscordBotService bot,
		TimedTaskQueue queue)
{}
