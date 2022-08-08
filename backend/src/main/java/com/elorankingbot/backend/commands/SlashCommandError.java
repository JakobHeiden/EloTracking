package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlashCommandError extends SlashCommand {

	private final Exception exception;

	public SlashCommandError(ChatInputInteractionEvent event, Services services, Exception exception) {
		super(event, services);
		this.exception = exception;
	}

	protected void execute() {
		event.reply(exception.getMessage() + ".\nI sent a report to the developer.").subscribe();
		String errorReport = String.format("Error executing %s on %s by %s: %s", event.getCommandName(),
				server.getName(bot), event.getInteraction().getUser().getTag(), exception.getMessage());
		log.error(errorReport);
		bot.sendToOwner(errorReport);
		exception.printStackTrace();
	}
}
