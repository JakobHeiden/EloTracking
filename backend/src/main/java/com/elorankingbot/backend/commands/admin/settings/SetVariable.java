package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.commands.Command;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
import discord4j.discordjson.possible.Possible;

import java.util.Optional;

import static com.elorankingbot.backend.commands.admin.settings.SettingsComponents.*;

@AdminCommand
public class SetVariable extends Command {

	private ModalSubmitInteractionEvent event;

	public SetVariable(ModalSubmitInteractionEvent event, Services services) {
		super(event, services);
		this.event = event;
	}

	protected void execute() {
		String gameName = event.getCustomId().split(":")[1];
		String variableName = event.getCustomId().split(":")[2];
		String value = event.getComponents(TextInput.class).get(0).getValue().get();

		Game game = server.getGame(gameName);
		Optional<String> maybeErrorMessage = game.setVariable(variableName, value);
		String userFeedback;
		if (maybeErrorMessage.isPresent()) {
			userFeedback = String.format("\nError: %s -> %s: %s", variableName, value, maybeErrorMessage.get());
		} else {
			dbService.saveServer(server);
			if (variableName.equals("Name")) {// TODO die ganze fallunterscheidung von Game nach hier
				discordCommandManager.updateRankingCommands(server, exceptionHandler.createUpdateCommandFailedCallbackFactory(event));
				channelManager.refreshLeaderboard(game);// TODO channel ggf umbenennen...
			}
			userFeedback = String.format("\n**Variable %s for ranking %s is now set to %s.**", variableName, gameName, value);
		}
		event.getMessage().get().edit()
				.withContent(Possible.of(Optional.of(event.getMessage().get().getContent() + userFeedback)))
				.withEmbeds(gameSettingsEmbed(game))
				.withComponents(createVariableMenu(game), exitAndEscapeButton()).subscribe();
		acknowledgeEvent();
	}
}
