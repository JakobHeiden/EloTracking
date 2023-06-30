package com.elorankingbot.commands.mod.dispute;

import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsDraw extends RuleAsWinOrDraw {

	public RuleAsDraw(ButtonInteractionEvent event, Services services) {
		super(event, services);
		super.isRuleAsWin = false;
	}
}
