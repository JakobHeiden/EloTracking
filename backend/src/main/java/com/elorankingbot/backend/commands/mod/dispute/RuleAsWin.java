package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsWin extends RuleAsWinOrDraw {

	public RuleAsWin(ButtonInteractionEvent event, Services services) {
		super(event, services);
		super.winningTeamIndex = Integer.parseInt(event.getCustomId().split(":")[2]);
		super.isRuleAsWin = true;
	}
}
