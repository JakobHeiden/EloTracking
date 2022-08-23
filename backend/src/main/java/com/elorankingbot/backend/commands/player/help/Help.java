package com.elorankingbot.backend.commands.player.help;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.PlayerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import static com.elorankingbot.backend.commands.player.help.HelpComponents.*;

@Slf4j
@PlayerCommand
public class Help extends SlashCommand {

	private final Services services;
	private final CommandClassScanner commandClassScanner;

	public Help(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.services = services;
		this.commandClassScanner = services.commandClassScanner;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(Help.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true)
				.build();
	}

	public static String getShortDescription() {
		return "Get help on how to use the bot.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"The command will display some general help, and a menu to to display help on selected topics, and every bot command.";
	}

	protected void execute() throws Exception {
		String initialTopic = "General Help";
		event.reply().withEmbeds(createHelpEmbed(services, initialTopic))
				.withComponents(
						createConceptsActionRow(),
						createPlayerCommandsActionRow(commandClassScanner),
						createModCommandsActionRow(commandClassScanner),
						createAdminCommandsActionRow(commandClassScanner))
				.block();
	}
}
