package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import static com.elorankingbot.backend.model.ReportStatus.DRAW;

public class Draw extends Report {

	public Draw(ButtonInteractionEvent event, Services services) {
		super(event, services, DRAW);
	}
}
