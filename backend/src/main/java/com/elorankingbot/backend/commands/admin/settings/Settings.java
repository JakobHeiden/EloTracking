package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.command.annotations.GlobalCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.commands.admin.AddQueue;
import com.elorankingbot.backend.commands.admin.CreateRanking;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

import static com.elorankingbot.backend.commands.admin.settings.SettingsComponents.*;

@AdminCommand
@GlobalCommand
public class Settings extends SlashCommand {

	public Settings(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(Settings.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true)
				.build();
	}

	public static String getShortDescription() {
		return "Open the settings menu for the rankings on the server.";
	}

	public static String getLongDescription() {
		return getShortDescription();
	}

	protected void execute() {
		if (server.getGames().isEmpty()) {
			event.reply(String.format("There are no rankings yet. Use `/%s` to create a ranking.",
					CreateRanking.class.getSimpleName().toLowerCase())).subscribe();
			return;
		}
		// this is for now necessary as there are game settings mixed with queue settings. possibly remove this at a later point.
		// see SettingsComponents::gameSettingsEmbed
		if (server.getQueues().isEmpty()) {
			event.reply(String.format("There are no queues yet. Use `/%s` to add a queue to a ranking.",
					AddQueue.class.getSimpleName().toLowerCase())).subscribe();
			return;
		}

		event.reply("Welcome to the settings menu.")
				.withEmbeds(allGamesSettingsEmbeds(server))
				.withComponents(selectGameMenu(server), exitButton()).subscribe();
	}
}
