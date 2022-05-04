package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class DeleteQueue extends SlashCommand {

	public DeleteQueue(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}



	protected void execute() {

	}
}
