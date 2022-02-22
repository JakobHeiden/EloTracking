package com.elorankingbot.backend.commands.dispute;

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
		/*
		if (!isByModeratorOrAdmin()) return;

		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getChallengerUserId());
		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getAcceptorUserId());

		Player challengerPlayer = service.findPlayerByGuildIdAndUserId(guildId, challenge.getChallengerUserId()).get();
		Player acceptorPlayer = service.findPlayerByGuildIdAndUserId(guildId, challenge.getAcceptorUserId()).get();
		Match match;
		if (challengerPlayer.getRating() < acceptorPlayer.getRating()) {// in a draw the lower ranked player will gain rating
			match = new Match(guildId, challenge.getChallengerUserId(), challenge.getAcceptorUserId(),
					challenge.getChallengerTag(), challenge.getAcceptorTag(), true);
		} else {
			match = new Match(guildId, challenge.getAcceptorUserId(), challenge.getChallengerUserId(),
					challenge.getAcceptorTag(), challenge.getChallengerTag(), true);
		}
		eloResults = service.updateRatingsAndSaveMatchAndPlayers(match);
		service.saveMatch(match);

		postToDisputeChannel(String.format(
				"%s has ruled the match a draw :left_right_arrow: for <@%s> and <@%s>.",
				moderatorName, match.getWinnerId(), match.getLoserId()));
		bot.postToResultChannel(game, match);
		bot.updateLeaderboard(game);
		updateMessages();

		service.deleteChallenge(challenge);

		addMatchSummarizeToQueue(match);
		event.acknowledge().subscribe();

		 */
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
