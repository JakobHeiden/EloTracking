package com.elorankingbot.backend.command_legacy;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTask;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class DeleteChannelLater extends ButtonCommand {

	public DeleteChannelLater(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		Server server = dbService.findServerByGuildId(event.getInteraction().getGuildId().get().asLong()).get();
		if (!event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getAdminRoleId()))
				&& !event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getModRoleId()))) {
			event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
			return;
		}

		event.acknowledge().subscribe();
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
		event.getInteraction().getChannel()
				.subscribe(channel -> channelManager.moveToArchive(server, channel));

		timedTaskQueue.addTimedTask(
				TimedTask.TimedTaskType.CHANNEL_DELETE, 24 * 60,
				event.getInteraction().getChannelId().asLong(),
				0L, null);
	}
}
