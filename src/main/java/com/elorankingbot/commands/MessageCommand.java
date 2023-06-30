package com.elorankingbot.commands;

import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;

public abstract class MessageCommand extends Command {

	protected final MessageInteractionEvent event;

	protected MessageCommand(MessageInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}
}
