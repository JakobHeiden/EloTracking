package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.tools.MessageUpdater;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Ruleasdraw extends ButtonCommandRelatedToDispute {

	private double[] eloResults;
	private Match match;

	public Ruleasdraw(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		match = new Match(challenge.getGuildId(), challenge.getChallengerId(), challenge.getAcceptorId(), true);
		eloResults = service.updateRatings(match);
		service.saveMatch(match);
		service.deleteChallenge(challenge);

		postToDisputeChannel(String.format(
				"%s has ruled the match a draw :left_right_arrow: for <@%s> and <@%s>.",
				moderatorName, challenge.getChallengerId(), challenge.getAcceptorId()));
		bot.postToResultChannel(game, match);
		new MessageUpdater(challengerMessage)
				.addLine(String.format("%s has ruled this as a draw :left_right_arrow:.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(acceptorMessage)
				.addLine(String.format("%s has ruled this as a draw :left_right_arrow:.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();

		addMatchSummarizeToQueue(match);
		event.acknowledge().subscribe();
	}
}
