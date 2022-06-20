package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends RuleAsWinOrDraw {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		String reason = String.format("%s has ruled the match to be canceled.", moderatorTag);
		MatchResult canceledMatchResult = MatchService.generateCanceledMatchResult(match);
		matchService.processCancel(canceledMatchResult, match, reason);
		removeButtons();
		postToDisputeChannel("**" + reason + "**").block();
		event.getInteraction().getChannel().subscribe(channel -> bot.moveToArchive(server, channel));
		event.acknowledge().subscribe();
	}
}
