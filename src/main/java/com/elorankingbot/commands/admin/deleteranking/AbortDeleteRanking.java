package com.elorankingbot.commands.admin.deleteranking;

import com.elorankingbot.commands.ButtonCommand;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class AbortDeleteRanking extends ButtonCommand {

	public AbortDeleteRanking(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		long userIdWhoClicked = event.getInteraction().getUser().getId().asLong();
		long userIdWhoCalledDeleteRanking = Long.parseLong(event.getCustomId().split(":")[2]);
		if (userIdWhoClicked != userIdWhoCalledDeleteRanking) {
			event.reply("Only the user who used `/deleteranking` can use this button.")
					.withEphemeral(true).subscribe();
			return;
		}

		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();
		event.reply("Aborting.").subscribe();
	}
}
