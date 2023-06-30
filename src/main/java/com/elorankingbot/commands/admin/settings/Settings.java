package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import com.elorankingbot.timedtask.DurationParser;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.stream.Collectors;

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
		return "Open the settings menu for the server.";
	}

	public static String getLongDescription() {
		return getShortDescription();
	}

	protected void execute() {
		event.reply("Welcome to the settings menu.")
				.withEmbeds(serverSettingsEmbed(server))
				.withComponents(SelectServerVariableOrGame.menu(server), ActionRow.of(Exit.button())).subscribe();
	}

	static EmbedCreateSpec serverSettingsEmbed(Server server) {
		String gamesAsString = server.getGames().stream().map(Game::getName)
				.collect(Collectors.joining(", "));
		if (gamesAsString.equals("")) gamesAsString = "No rankings";
		String autoLeaveQueuesAsString = server.getAutoLeaveQueuesAfter() == Server.NEVER ? "never"
				: DurationParser.minutesToString(server.getAutoLeaveQueuesAfter());
		return EmbedCreateSpec.builder()
				.title("Server settings and rankings")
				.addField(EmbedCreateFields.Field.of(AutoLeaveModal.variableName, autoLeaveQueuesAsString, false))
				.addField(EmbedCreateFields.Field.of("Rankings", gamesAsString, false))
				.build();
	}
}
