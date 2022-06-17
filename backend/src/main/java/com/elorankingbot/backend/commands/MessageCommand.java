package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;

public abstract class MessageCommand extends Command {

	protected final MessageInteractionEvent event;

	protected MessageCommand(MessageInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}

	public static String getCommandName(Class thisClass) {
		String regex = "([a-z])([A-Z])";
		String replacement = "$1 $2";
		return thisClass.getSimpleName().replaceAll(regex, replacement);
	}
}
