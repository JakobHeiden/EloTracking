package com.elorankingbot.backend.commands.dispute;

import com.elorankingbot.backend.model.Match;
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

		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getChallengerId());
		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getAcceptorId());
		isChallengerWin = event.getCustomId().split(":")[2].equals("true");
		long winnerId = isChallengerWin ? challenge.getChallengerId() : challenge.getAcceptorId();
		long loserId = isChallengerWin ? challenge.getAcceptorId() : challenge.getChallengerId();
		String winnerTag = isChallengerWin ? challenge.getChallengerTag() : challenge.getAcceptorTag();
		String loserTag = isChallengerWin ? challenge.getAcceptorTag() : challenge.getChallengerTag();

		Match match = new Match(challenge.getGuildId(), winnerId, loserId, winnerTag, loserTag, false);
		eloResults = service.updateRatingsAndSaveMatchAndPlayers(match);
		service.saveMatch(match);

		postToDisputeChannel(String.format(
				"%s has ruled this match a win :arrow_up: for <@%s> and a loss :arrow_down: for <@%s>.",
				moderatorName, winnerId, loserId));
		bot.postToResultChannel(game, match);
		bot.updateLeaderboard(game);
		updateMessages();

		service.deleteChallenge(challenge);

		addMatchSummarizeToQueue(match);
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
