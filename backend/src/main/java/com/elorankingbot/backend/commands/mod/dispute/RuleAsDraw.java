package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.FormatTools;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsDraw extends ButtonCommandRelatedToDispute {

	private double[] eloResults;

	public RuleAsDraw(ButtonInteractionEvent event, Services services) {
		super(event, services);
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
						FormatTools.formatRating(eloResults[0]), FormatTools.formatRating(eloResults[2])))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.doOnNext(super::updateChallengerMessageIdAndSaveChallenge)
				.block();
		new MessageUpdater(acceptorMessage)
				.addLine(String.format("%s has ruled this as a draw :left_right_arrow:.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						FormatTools.formatRating(eloResults[1]), FormatTools.formatRating(eloResults[3])))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.subscribe(super::updateAcceptorMessageIdAndSaveChallenge);
	}
}
