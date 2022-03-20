package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

public class RuleAsCancel extends ButtonCommandRelatedToDispute {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!isByModeratorOrAdminDoReply()) return;

		dbservice.deleteMatch(match);

		postToDisputeChannelAndUpdateButtons(String.format("%s has ruled the match to be canceled.", moderatorName));

		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						String embedTitle = String.format("%s has ruled the match to be canceled.", moderatorName);
						EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(embedTitle, match, player.getTag());
						message.delete().subscribe();
						bot.getPrivateChannelByUserId(player.getUserId()).subscribe(channel ->
								channel.createMessage(embedCreateSpec).subscribe());
					});
		}

		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), null);
		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), null);
		event.acknowledge().subscribe();
	}
}
