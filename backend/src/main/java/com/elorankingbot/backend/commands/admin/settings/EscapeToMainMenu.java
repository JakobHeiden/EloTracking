package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.components.Emojis;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;

public class EscapeToMainMenu extends ButtonCommand {

	private static final String customId = EscapeToMainMenu.class.getSimpleName().toLowerCase();
	public EscapeToMainMenu(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	static Button button() {
		return Button.secondary(customId, Emojis.redoArrow, "Back to main menu");
	}

	public void execute() {
		Server server = dbService.getOrCreateServer(event.getInteraction().getGuildId().get().asLong());
		event.getMessage().get().edit()
				.withEmbeds(Settings.allGamesSettingsEmbeds(server))
				.withComponents(SelectGame.menu(server), ActionRow.of(Exit.button())).subscribe();
		acknowledgeEvent();
	}
}
