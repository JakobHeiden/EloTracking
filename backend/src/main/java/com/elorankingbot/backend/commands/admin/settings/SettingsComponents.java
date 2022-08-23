package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.components.Buttons;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Server;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

public class SettingsComponents {

	static List<EmbedCreateSpec> allGamesSettingsEmbeds(Server server) {
		return server.getGames().stream().map(SettingsComponents::gameSettingsEmbed).toList();
	}

	static EmbedCreateSpec gameSettingsEmbed(Game game) {
		return EmbedCreateSpec.builder()
				.title("Ranking: " + game.getName())
				.addField(EmbedCreateFields.Field.of("Name", game.getName(), true))
				.addField(EmbedCreateFields.Field.of("Initial rating", String.valueOf(game.getInitialRating()), true))
				// TODO das hier ist eigentlich ein setting an queue
				.addField(EmbedCreateFields.Field.of("K", String.valueOf(game.getQueues().stream().findAny().get().getK()), true))
				.build();
	}

	static ActionRow selectGameMenu(Server server) {
		List<SelectMenu.Option> rankingsOptions = new ArrayList<>();
		server.getGames().forEach(game -> rankingsOptions.add(SelectMenu.Option.of(game.getName(), game.getName())));
		if (rankingsOptions.isEmpty()) {
			rankingsOptions.add(SelectMenu.Option.of("No rankings yet.", "-norankingsyet"));// dash is not allowed in game name
		}
		return ActionRow.of(SelectMenu.of(SelectGame.class.getSimpleName().toLowerCase(), rankingsOptions)
				.withPlaceholder("Select a ranking to edit"));
	}

	static ActionRow createVariableMenu(Game game) {
		List<SelectMenu.Option> variableMenu = List.of(
				// SelectMenu.Option.of("Name", game.getName() + ":Name"),
				SelectMenu.Option.of("Initial Rating", game.getName() + ":Initial Rating"),
				SelectMenu.Option.of("K", game.getName() + ":K")
		);
		return ActionRow.of(SelectMenu.of(SelectGameSetting.class.getSimpleName().toLowerCase(), variableMenu)
				.withPlaceholder("Select a setting to edit"));
	}

	static ActionRow exitButton() {
		return ActionRow.of(Buttons.exit());
	}

	static ActionRow exitAndEscapeButton() {
		return ActionRow.of(Buttons.exit(), Buttons.escape());
	}
}
