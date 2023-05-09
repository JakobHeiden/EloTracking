package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.commands.SelectMenuCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;

import java.util.ArrayList;
import java.util.List;

@AdminCommand
public class SelectGame extends SelectMenuCommand {

	public static String customId = SelectGame.class.getSimpleName().toLowerCase();

	public SelectGame(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
	}

	static ActionRow menu(Server server) {
		List<SelectMenu.Option> rankingsOptions = new ArrayList<>();
		server.getGames().forEach(game -> rankingsOptions.add(SelectMenu.Option.of(game.getName(), game.getName())));
		return ActionRow.of(SelectMenu.of(customId, rankingsOptions)
				.withPlaceholder("Select a ranking to edit"));
	}

	protected void execute() {
		Game game = server.getGame(event.getValues().get(0));
		event.getMessage().get().edit()
				.withEmbeds(Settings.gameSettingsEmbed(game))
				.withComponents(SelectGameVariableOrQueue.menu(game), ActionRow.of(Exit.button(), EscapeToMainMenu.button())).subscribe();
		acknowledgeEvent();
	}
}
