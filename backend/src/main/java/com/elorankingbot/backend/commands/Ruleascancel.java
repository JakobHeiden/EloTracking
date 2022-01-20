package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.tools.MessageUpdater;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Ruleascancel extends ButtonCommandRelatedToDispute {

	public Ruleascancel(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		service.deleteChallenge(challenge);

		postToDisputeChannel(String.format(
				"%s has ruled the challenge to be canceled. <@%s> <@%s>",
				moderatorName, challenge.getChallengerId(), challenge.getAcceptorId()));
		new MessageUpdater(challengerMessage)
				.addLine(String.format("%s has ruled this to be canceled :negative_squared_cross_mark:.", moderatorName))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(acceptorMessage)
				.addLine(String.format("%s has ruled this to be canceled :negative_squared_cross_mark:.", moderatorName))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), null);
		event.acknowledge().subscribe();
	}
}
