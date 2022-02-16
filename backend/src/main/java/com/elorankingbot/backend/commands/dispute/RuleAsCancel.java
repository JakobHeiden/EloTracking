package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends ButtonCommandRelatedToDispute {

	public RuleAsCancel(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		service.deleteChallenge(challenge);

		postToDisputeChannel(String.format(
				"%s has ruled the challenge to be canceled. <@%s> <@%s>",
				moderatorName, challenge.getChallengerUserId(), challenge.getAcceptorUserId()));
		new MessageUpdater(challengerMessage)
				.addLine(String.format("%s has ruled this match to be canceled :negative_squared_cross_mark:.", moderatorName))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.block();
		new MessageUpdater(acceptorMessage)
				.addLine(String.format("%s has ruled this match to be canceled :negative_squared_cross_mark:.", moderatorName))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.subscribe();

		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), null);
		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), null);
		event.acknowledge().subscribe();
	}
}
