package com.elorankingbot.commands.player.match;

import com.elorankingbot.model.ReportStatus;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Cancel extends Report {

	public Cancel(ButtonInteractionEvent event, Services services) {
		super(event, services, ReportStatus.CANCEL);
	}
}
