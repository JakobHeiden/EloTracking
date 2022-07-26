package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.command.NoHelpEntry;
import com.elorankingbot.backend.commands.SelectMenuCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.TextInput;

import java.util.Collection;
import java.util.List;

@AdminCommand
@NoHelpEntry
public class SelectGameSetting extends SelectMenuCommand {

	private String selectedVariable;
	private Game game;

	public SelectGameSetting(SelectMenuInteractionEvent event, Services services) {
		super(event, services);
	}

	protected void execute() {
		game = server.getGame(event.getValues().get(0).split(":")[0]);
		selectedVariable = event.getValues().get(0).split(":")[1];
		event.presentModal(
				String.format("Enter a new value for: %s", selectedVariable),
				String.format("%s:%s:%s", SetVariable.class.getSimpleName().toLowerCase(), game.getName(), selectedVariable),
				textInput()
		).subscribe();
	}

	private Collection<LayoutComponent> textInput() {
		String selectedVariableCurrentValue = game.getVariable(selectedVariable);
		return List.of(ActionRow.of(TextInput.small(
				"none",
				String.format("Current value: %s", selectedVariableCurrentValue))));
	}
}
