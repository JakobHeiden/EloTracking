package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;

public abstract class SelectMenuCommand extends Command {

	protected SelectMenuInteractionEvent event;

	protected SelectMenuCommand(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}
}
