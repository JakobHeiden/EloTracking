package com.elorankingbot.backend.command;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

@UseToStringForLogging
record ButtonInteractionEventWrapper(
		ButtonInteractionEvent event,
		EloRankingService service,
		DiscordBotService bot,
		TimedTaskQueue queue,
		GatewayDiscordClient client)
{
	public String toString() {
		return event.getCustomId();
	}
}
