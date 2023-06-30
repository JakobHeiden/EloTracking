package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.commands.SelectMenuCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.MatchFinderQueue;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AdminCommand
public class SelectServerVariableOrGame extends SelectMenuCommand {

	public static String customId = SelectServerVariableOrGame.class.getSimpleName().toLowerCase();
	private static final String AUTO_LEAVE_ID = ":autoleave";

	public SelectServerVariableOrGame(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
	}

	static ActionRow menu(Server server) {
		List<SelectMenu.Option> options = new ArrayList<>();
		options.add(SelectMenu.Option.of(AutoLeaveModal.variableName, AUTO_LEAVE_ID));
		server.getGames().forEach(game -> options.add(SelectMenu.Option.of("Ranking: " + game.getName(), game.getName())));
		return ActionRow.of(SelectMenu.of(customId, options)
				.withPlaceholder("Select a variable or a ranking to edit"));
	}

	protected void execute() {
		if (event.getValues().get(0).equals(AUTO_LEAVE_ID)) {
			processSetAutoLeave();
		} else {
			processGameSelected();
		}
	}

	private void processSetAutoLeave() {
		AutoLeaveModal.present(server, event);
	}

	private void processGameSelected() {
		Game game = server.getGame(event.getValues().get(0));
		event.getMessage().get().edit()
				.withEmbeds(gameSettingsEmbed(game))
				.withComponents(SelectGameVariableOrQueue.menu(game), ActionRow.of(Exit.button(), EscapeToMainMenu.button())).subscribe();
		acknowledgeEvent();
	}

	static EmbedCreateSpec gameSettingsEmbed(Game game) {
		String queuesAsString = game.getQueues().stream().map(MatchFinderQueue::getName)
				.collect(Collectors.joining(", "));
		if (queuesAsString.equals("")) queuesAsString = "No queues";
		return EmbedCreateSpec.builder()
				.title(game.getName())
				.addField(EmbedCreateFields.Field.of("Name", game.getName(), false))
				.addField(EmbedCreateFields.Field.of("Initial rating", String.valueOf(game.getInitialRating()), false))
				.addField(EmbedCreateFields.Field.of("Queues", queuesAsString, false))
				.build();
	}
}
