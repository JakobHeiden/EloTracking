package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.commands.SelectMenuCommand;
import com.elorankingbot.backend.commands.admin.CreateRanking;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.discordjson.possible.Possible;

import java.util.Optional;

import static com.elorankingbot.backend.commands.admin.settings.SettingsComponents.*;

@AdminCommand
public class SelectGame extends SelectMenuCommand {

	private Game game;

	public SelectGame(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
	}

	protected void execute() {
		if (event.getValues().get(0).equals("-norankingsyet")) {// - is not allowed in game name
			event.getMessage().get().edit().withContent(Possible.of(Optional.of(
					String.format("No rankings yet. Please create a ranking with `/%s`.",
							CreateRanking.class.getSimpleName().toLowerCase())))).subscribe();
			acknowledgeEvent();
			return;
		}

		game = server.getGame(event.getValues().get(0));
		event.getMessage().get().edit()
				.withEmbeds(gameSettingsEmbed(game))
				.withComponents(createVariableMenu(game), exitAndEscapeButton()).subscribe();
		acknowledgeEvent();
	}
}
