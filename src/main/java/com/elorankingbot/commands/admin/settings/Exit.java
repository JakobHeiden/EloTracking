package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.commands.ButtonCommand;
import com.elorankingbot.components.Emojis;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.discordjson.possible.Possible;

import java.util.Optional;

public class Exit extends ButtonCommand {

	public Exit(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	static Button button() {
		return Button.secondary(Exit.class.getSimpleName().toLowerCase(),
				Emojis.crossMark, "Close settings menu");
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
