package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class CloseChannelLater extends ButtonCommand {

	public CloseChannelLater(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
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
				TimedTask.TimedTaskType.CHANNEL_DELETE, 1,// 24 * 60,
				event.getInteraction().getChannelId().asLong(),
				0L, null);
		event.acknowledge().subscribe();
	}
}
