package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends RuleAsWinOrDraw {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		String reason = String.format("%s has ruled the match to be canceled.", moderatorTag);
		matchService.processCancel(match, reason);
		removeButtons();
		postToDisputeChannel("**" + reason + "**").block();
		event.getInteraction().getChannel().subscribe(channel -> bot.moveToArchive(server, channel));
		event.acknowledge().subscribe();
	}
}
