package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends ButtonCommandRelatedToDispute {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!isByModeratorOrAdminDoReply()) return;

		//TODO!
		/*

		dbservice.deleteChallenge(challenge);

		postToDisputeChannelAndUpdateButtons(String.format(
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

		 */
	}
}
