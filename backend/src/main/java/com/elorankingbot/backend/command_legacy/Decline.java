package com.elorankingbot.backend.command_legacy;

import com.elorankingbot.backend.commands.player.match.ButtonCommandRelatedToMatch;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Decline extends ButtonCommandRelatedToMatch {

	public Decline(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		//service.deleteChallengeById(challenge.getId());

		/*
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

		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), null);
		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), null);

		 */
	}
}
