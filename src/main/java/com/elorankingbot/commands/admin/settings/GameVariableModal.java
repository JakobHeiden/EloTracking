package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.FormatTools;
import com.elorankingbot.command.DiscordCommandManager;
import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.commands.ModalSubmitCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.TextInput;
import discord4j.discordjson.possible.Possible;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@AdminCommand
public class GameVariableModal extends ModalSubmitCommand {

	private final DiscordCommandManager discordCommandManager;
	public static final String customId = GameVariableModal.class.getSimpleName().toLowerCase();

	public GameVariableModal(ModalSubmitInteractionEvent event, Services services) {
		super(event, services);
		this.discordCommandManager = services.discordCommandManager;
	}

	static void present(Server server, SelectMenuInteractionEvent event) {
		Game game = server.getGame(event.getValues().get(0).split(":")[0]);
		String selectedVariable = event.getValues().get(0).split(":")[1];
		event.presentModal(
				String.format("Enter a new value for: %s", selectedVariable),
				String.format("%s:%s:%s", GameVariableModal.customId, game.getName(), selectedVariable),
				textInputForSetGameVariable(game, selectedVariable)
		).subscribe();
	}

	private static Collection<LayoutComponent> textInputForSetGameVariable(Game game, String selectedVariable) {
		String selectedVariableCurrentValue = getValueFromGame(game, selectedVariable);
		return List.of(ActionRow.of(TextInput.small(
				GameVariableModal.customId,// TODO!
				String.format("Current value: %s", selectedVariableCurrentValue))));
	}

	private static String getValueFromGame(Game game, String variableName) {
		switch (variableName) {
			case "Name" -> {
				return game.getName();
			}
			case "Initial Rating" -> {
				return String.valueOf(game.getInitialRating());
			}
			default -> {
				return "error";
			}
		}
	}

	protected void execute() {
		String gameName = event.getCustomId().split(":")[1];
		String variableName = event.getCustomId().split(":")[2];
		String value = event.getComponents(TextInput.class).get(0).getValue().get();

		Game game = server.getGame(gameName);
		Optional<String> maybeErrorMessage = setValue(game, variableName, value);
		String userFeedback;
		if (maybeErrorMessage.isPresent()) {
			userFeedback = String.format("\nError: %s -> %s: %s", variableName, value, maybeErrorMessage.get());
		} else {
			dbService.saveServer(server);
			if (variableName.equals("Name")) {
				discordCommandManager.updateGameCommands(server, exceptionHandler.updateCommandFailedCallbackFactory(event));
				channelManager.refreshLeaderboard(game);
				channelManager.updateLeaderboardChannelName(game);
			}
			userFeedback = String.format("\n**Variable %s for ranking %s is now set to %s.**", variableName, gameName, value);
		}
		event.getMessage().get().edit()
				.withContent(Possible.of(Optional.of(event.getMessage().get().getContent() + userFeedback)))
				.withEmbeds(SelectServerVariableOrGame.gameSettingsEmbed(game))
				.withComponents(SelectGameVariableOrQueue.menu(game), ActionRow.of(Exit.button(), EscapeToMainMenu.button())).subscribe();
		acknowledgeEvent();
	}

	private Optional<String> setValue(Game game, String variableName, String value) {
		switch (variableName) {
			case "Name" -> {
				if (!FormatTools.isLegalDiscordName(value)) {
					return Optional.of(FormatTools.illegalNameMessage());
				}
				server.getGameNameToGame().remove(game.getName());
				game.setName(value);
				game.getQueues().forEach(queue -> queue.setGame(game));
				server.getGameNameToGame().put(game.getName(), game);
				return Optional.empty();
			}
			case "Initial Rating" -> {
				try {
					game.setInitialRating(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					return Optional.of("Please enter an Integer.");
				}
				return Optional.empty();
			}
			default -> {
				return Optional.of("error");
			}
		}
	}
}
