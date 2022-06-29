package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import static com.elorankingbot.backend.commands.admin.settings.Components.*;

public class Escape extends ButtonCommand {

	public Escape(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		Server server = dbService.getServerByGuildId(event.getInteraction().getGuildId().get().asLong());
		event.getMessage().get().edit()
				.withEmbeds(allGamesSettingsEmbeds(server))
				.withComponents(selectGameMenu(server), exitButton()).subscribe();
		acknowledgeEvent();
	}
}
