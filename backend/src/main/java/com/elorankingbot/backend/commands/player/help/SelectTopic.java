package com.elorankingbot.backend.commands.player.help;

import com.elorankingbot.backend.command.annotations.PlayerCommand;
import com.elorankingbot.backend.commands.SelectMenuCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;

import static com.elorankingbot.backend.commands.player.help.HelpComponents.createHelpEmbed;

@PlayerCommand
public class SelectTopic extends SelectMenuCommand {

	private final Services services;
	static final String customId = SelectTopic.class.getSimpleName().toLowerCase();

	public SelectTopic(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
		this.services = services;
	}

	protected void execute() throws Exception {
		event.getMessage().get().edit().withEmbeds(createHelpEmbed(services, event.getValues().get(0)))
				.doOnError(super::forwardToExceptionHandler)
				.subscribe();
		event.deferEdit().subscribe();
	}
}
