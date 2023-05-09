package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.command.annotations.GlobalCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.commands.admin.CreateRanking;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchFinderQueue;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;
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

		event.reply("Welcome to the settings menu.")
				.withEmbeds(allGamesSettingsEmbeds(server))
				.withComponents(SelectGame.menu(server), ActionRow.of(Exit.button())).subscribe();
	}

	// TODO!
	//  zeugs aus Edit reinziehen. testen. was noch einpflegen?

	static List<EmbedCreateSpec> allGamesSettingsEmbeds(Server server) {
		return server.getGames().stream().map(Settings::gameSettingsEmbed).toList();
	}

	static EmbedCreateSpec gameSettingsEmbed(Game game) {
		String queuesAsString = game.getQueues().stream().map(MatchFinderQueue::getName)
				.collect(Collectors.joining(", "));
		if (queuesAsString.equals("")) queuesAsString = "No queues";
		return EmbedCreateSpec.builder()
				.title(game.getName())
				.addField(EmbedCreateFields.Field.of("Name", game.getName(), true))
				.addField(EmbedCreateFields.Field.of("Initial rating", String.valueOf(game.getInitialRating()), true))
				.addField(EmbedCreateFields.Field.of("Queues", queuesAsString, true))
				.build();
	}
}
