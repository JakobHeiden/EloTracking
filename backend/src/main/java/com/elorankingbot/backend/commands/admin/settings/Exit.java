package com.elorankingbot.backend.commands.admin.settings;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.discordjson.possible.Possible;

import java.util.Optional;

public class Exit extends ButtonCommand {

	public Exit(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		event.getMessage().get().edit()
				.withContent(Possible.of(Optional.of(event.getMessage().get().getContent() +
						"\nClosing settings menu.")))
				.withEmbeds(none)
				.withComponents(none).subscribe();
		acknowledgeEvent();
	}
}
