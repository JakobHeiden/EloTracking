package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsDraw extends ButtonCommandRelatedToDispute {

	private double[] eloResults;

	public RuleAsDraw(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getChallengerId());
		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getAcceptorId());
		Match match = new Match(challenge.getGuildId(), challenge.getChallengerId(), challenge.getAcceptorId(), true);
		eloResults = service.updateRatingsAndSaveMatchAndPlayers(match);
		service.saveMatch(match);

		postToDisputeChannel(String.format(
				"%s has ruled the match a draw :left_right_arrow: for <@%s> and <@%s>.",
				moderatorName, challenge.getChallengerId(), challenge.getAcceptorId()));
		bot.postToResultChannel(game, match);
		bot.updateLeaderboard(game);
		updateMessages();

		service.deleteChallenge(challenge);

		addMatchSummarizeToQueue(match);
		event.acknowledge().subscribe();
	}

	private void updateMessages() {
		new MessageUpdater(challengerMessage)
				.addLine(String.format("%s has ruled this as a draw :left_right_arrow:.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.doOnNext(super::updateChallengerMessageIdAndSaveChallenge)
				.block();
		new MessageUpdater(acceptorMessage)
				.addLine(String.format("%s has ruled this as a draw :left_right_arrow:.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.subscribe(super::updateAcceptorMessageIdAndSaveChallenge);
	}
}
