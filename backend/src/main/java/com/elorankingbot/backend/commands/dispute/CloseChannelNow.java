package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class CloseChannelNow extends ButtonCommand {

	public CloseChannelNow(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		/*
		Game game = service.findGameByGuildId(event.getInteraction().getGuildId().get().asLong()).get();
		if (!event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))
				&& !event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getModRoleId()))) {
			event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
			return;
		}

		event.getInteraction().getChannel().block().delete().subscribe();
		event.acknowledge().subscribe();

		 */
	}
}
