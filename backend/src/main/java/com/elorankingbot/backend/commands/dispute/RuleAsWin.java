package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

public class RuleAsWin extends ButtonCommandRelatedToDispute {

	private double[] eloResults;
	private boolean isChallengerWin;

	public RuleAsWin(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getChallengerUserId());
		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getAcceptorUserId());
		isChallengerWin = event.getCustomId().split(":")[2].equals("true");
		long winnerId = isChallengerWin ? challenge.getChallengerUserId() : challenge.getAcceptorUserId();
		long loserId = isChallengerWin ? challenge.getAcceptorUserId() : challenge.getChallengerUserId();
		String winnerTag = isChallengerWin ? challenge.getChallengerTag() : challenge.getAcceptorTag();
		String loserTag = isChallengerWin ? challenge.getAcceptorTag() : challenge.getChallengerTag();

		MatchResult matchResult = null;//new Match(challenge.getGuildId(), winnerId, loserId, winnerTag, loserTag, false);
		eloResults = service.updateRatingsAndSaveMatchAndPlayers(matchResult);
		service.saveMatch(matchResult);

		postToDisputeChannel(String.format(
				"%s has ruled this match a win :arrow_up: for <@%s> and a loss :arrow_down: for <@%s>.",
				moderatorName, winnerId, loserId));
		bot.postToResultChannel(game, matchResult);
		bot.updateLeaderboard(game);
		updateMessages();

		service.deleteChallenge(challenge);

		addMatchSummarizeToQueue(matchResult);
		event.acknowledge().subscribe();
	}

	private void updateMessages() {
		Message winnerMessage = isChallengerWin ? challengerMessage : acceptorMessage;
		winnerMessage = new MessageUpdater(winnerMessage)
				.addLine(String.format("%s has ruled this match as a win :arrow_up: for you.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.block();
		Message loserMessage = isChallengerWin ? acceptorMessage : challengerMessage;
		loserMessage = new MessageUpdater(loserMessage)
				.addLine(String.format("%s has ruled this match as a loss :arrow_down: for you.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s",
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic()
				.resend()
				.withComponents(none)
				.block();
		if (isChallengerWin) {
			super.updateChallengerMessageIdAndSaveChallenge(winnerMessage);
			super.updateAcceptorMessageIdAndSaveChallenge(loserMessage);
		} else {
			super.updateChallengerMessageIdAndSaveChallenge(loserMessage);
			super.updateAcceptorMessageIdAndSaveChallenge(winnerMessage);
		}
	}
}
