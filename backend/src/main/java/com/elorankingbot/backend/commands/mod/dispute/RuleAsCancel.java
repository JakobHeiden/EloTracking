package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends RuleAsWinOrDraw {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!isByModeratorOrAdminDoReply()) return;

		matchService.processCancel(match);
		postToDisputeChannelAndUpdateButtons(String.format("%s has ruled the match to be canceled.", moderatorTag));
		event.acknowledge().subscribe();
	}
}
