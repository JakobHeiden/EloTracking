package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import static com.elorankingbot.backend.model.ReportStatus.WIN;

public class Win extends Report {

	public Win(ButtonInteractionEvent event, Services services) {
		super(event, services, WIN);
	}
}
