package com.elorankingbot.backend.commands.player.help;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.annotations.GlobalCommand;
import com.elorankingbot.backend.command.annotations.PlayerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.apachecommons.CommonsLog;

import static com.elorankingbot.backend.commands.player.help.HelpComponents.*;

@CommonsLog
@PlayerCommand
@GlobalCommand
public class Help extends SlashCommand {

	private final DiscordBotService bot;
	private final CommandClassScanner commandClassScanner;

	public Help(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		bot = services.bot;
		commandClassScanner = services.commandClassScanner;
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
		event.reply().withEmbeds(createHelpEmbed(bot, commandClassScanner, initialTopic))
				.withComponents(
						createConceptsActionRow(),
						createPlayerCommandsActionRow(commandClassScanner),
						createModCommandsActionRow(commandClassScanner),
						createAdminCommandsActionRow(commandClassScanner))
				.block();
	}
}
