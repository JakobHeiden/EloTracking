package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.ReportStatus;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Cancel extends Report {

	public Cancel(ButtonInteractionEvent event, Services services) {
		super(event, services, ReportStatus.CANCEL);
	}
}
