package com.elorankingbot.commands;

import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public abstract class SelectMenuCommand extends Command {

	protected SelectMenuInteractionEvent event;

	protected SelectMenuCommand(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
		log.warn(String.format("SelectMenuCommand, customId: %s, value: %s", event.getCustomId(), event.getValues().get(0)));
		// TODO!
	}
}
