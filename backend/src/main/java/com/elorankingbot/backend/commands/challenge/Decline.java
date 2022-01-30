package com.elorankingbot.backend.commands.challenge;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Decline extends ButtonCommandRelatedToChallenge {

	public Decline(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
				   TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		service.deleteChallengeById(challenge.getId());

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You have declined :negative_squared_cross_mark: their challenge.")
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("They have declined :negative_squared_cross_mark: your challenge.")
				.makeAllItalic()
				.resend()
				.subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), null);;
	}
}
