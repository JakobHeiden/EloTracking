package com.elorankingbot.commands;

import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class SlashCommand extends Command {

	protected final ChatInputInteractionEvent event;

	protected SlashCommand(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}
}
