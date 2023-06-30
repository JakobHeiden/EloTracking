package com.elorankingbot.commands.player.help;

import com.elorankingbot.command.CommandClassScanner;
import com.elorankingbot.command.annotations.PlayerCommand;
import com.elorankingbot.commands.SelectMenuCommand;
import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;

import static com.elorankingbot.commands.player.help.HelpComponents.createHelpEmbed;

@PlayerCommand
public class SelectTopic extends SelectMenuCommand {

	private final DiscordBotService bot;
	private final CommandClassScanner commandClassScanner;
	static final String customId = SelectTopic.class.getSimpleName().toLowerCase();

	public SelectTopic(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
		bot = services.bot;
		commandClassScanner = services.commandClassScanner;
	}

	protected void execute() throws Exception {
		event.getMessage().get().edit().withEmbeds(createHelpEmbed(bot, commandClassScanner, event.getValues().get(0)))
				.doOnError(super::forwardToExceptionHandler)
				.subscribe();
		event.deferEdit().subscribe();
	}
}
