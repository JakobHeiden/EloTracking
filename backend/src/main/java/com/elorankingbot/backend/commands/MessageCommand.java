package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;

public abstract class MessageCommand extends Command {

	protected final MessageInteractionEvent event;

	protected MessageCommand(MessageInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}

	// this is needed for the Help entry as well as deploying the discord command
	public static String formatCommandName(Class thisClass) {
		String regex = "([a-z])([A-Z])";
		String replacement = "$1 $2";
		return thisClass.getSimpleName().replaceAll(regex, replacement);
	}
}
