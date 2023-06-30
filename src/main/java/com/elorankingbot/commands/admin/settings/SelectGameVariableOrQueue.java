package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.commands.SelectMenuCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.MatchFinderQueue;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

@AdminCommand
public class SelectGameVariableOrQueue extends SelectMenuCommand {

	public static final String customId = SelectGameVariableOrQueue.class.getSimpleName().toLowerCase();

	public SelectGameVariableOrQueue(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
	}

	static ActionRow menu(Game game) {
		List<SelectMenu.Option> gameOptions = new ArrayList<>();
		gameOptions.add(SelectMenu.Option.of("Name", game.getName() + ":Name"));
		gameOptions.add(SelectMenu.Option.of("Initial Rating", game.getName() + ":Initial Rating"));
		game.getQueues().forEach(queue -> gameOptions.add(
				SelectMenu.Option.of("queue: " + queue.getName(), game.getName() + "," + queue.getName())));
		return ActionRow.of(SelectMenu.of(customId, gameOptions)
				.withPlaceholder("Select a queue or a setting to edit"));
	}

	protected void execute() {
		if (event.getValues().get(0).contains(":")) {
			processVariableSelected();
		} else {
			processQueueSelected();
		}
	}

	private void processVariableSelected() {
		GameVariableModal.present(server, event);
	}

	private void processQueueSelected() {
		Game game = server.getGame(event.getValues().get(0).split(",")[0]);
		MatchFinderQueue queue = game.getQueue(event.getValues().get(0).split(",")[1]);
		event.getMessage().get().edit()
				.withEmbeds(queueSettingsEmbed(queue))
				.withComponents(SelectQueueVariable.menu(queue), ActionRow.of(Exit.button(), EscapeToGameMenu.button(game))).subscribe();
		acknowledgeEvent();
	}

	static EmbedCreateSpec queueSettingsEmbed(MatchFinderQueue queue) {
		return EmbedCreateSpec.builder()
				.title(queue.getGame().getName() + " - " + queue.getName())
				.addField(EmbedCreateFields.Field.of("Name", queue.getName(), false))
				.addField(EmbedCreateFields.Field.of("K", String.valueOf(queue.getK()), false))
				.addField(EmbedCreateFields.Field.of("max-rating-spread", queue.getMaxRatingSpreadAsString(), false))
				.addField(EmbedCreateFields.Field.of("rating-elasticity", String.valueOf(queue.getRatingElasticity()), false))
				.build();
	}
}
