package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class CloseChannelLater extends ButtonCommand {

	public CloseChannelLater(ButtonInteractionEvent event, Services services) {
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

		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
		event.getInteraction().getChannel().block()
				.createMessage("I will delete this channel in 24 hours.").subscribe();

		queue.addTimedTask(
				TimedTask.TimedTaskType.CHANNEL_DELETE, 24 * 60,
				event.getInteraction().getChannelId().asLong(),
				0L, null);
		event.acknowledge().subscribe();

		 */
	}
}
