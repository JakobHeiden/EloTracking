package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import static com.elorankingbot.backend.model.ReportStatus.LOSE;

public class Lose extends Report {

	public Lose(ButtonInteractionEvent event, Services services) {
		super(event, services, LOSE);
	}
}
