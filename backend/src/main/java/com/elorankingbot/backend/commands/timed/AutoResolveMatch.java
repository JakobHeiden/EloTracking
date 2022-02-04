package com.elorankingbot.backend.commands.timed;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;

public class AutoResolveMatch extends TimedCommand {

	private Message reportPresentMessage;
	private Message reportAbsentMessage;

	public AutoResolveMatch(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client,
							TimedTaskQueue queue, long challengeId, int time) {
		super(service, bot, queue, client, challengeId, time);
	}

	public void execute() {
		if (challenge == null) return;
		if (challenge.isDispute()) return;

		boolean hasChallengerReported = challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED;
		ChallengeModel.ReportStatus report = hasChallengerReported ?
				challenge.getChallengerReported()
				: challenge.getAcceptorReported();
		long challengerId = challenge.getChallengerId();
		long acceptorId = challenge.getAcceptorId();
		long winnerId = 0L;
		long loserId = 0L;
		boolean isDraw = false;
		boolean isWin = false;
		switch (report) {
			case WIN:
				winnerId = hasChallengerReported ? challengerId : acceptorId;
				loserId = hasChallengerReported ? acceptorId : challengerId;
				isWin = true;
				break;
			case LOSE:
				winnerId = hasChallengerReported ? acceptorId : challengerId;
				loserId = hasChallengerReported ? challengerId : acceptorId;
				break;
			case DRAW:
				winnerId = challengerId;
				loserId = acceptorId;
				isDraw = true;
				break;
			case CANCEL:
				autoResolveMatchAsCancel(challenge, hasChallengerReported);
				return;
		}

		Game game = service.findGameByGuildId(challenge.getGuildId()).get();
		Match match = new Match(challenge.getGuildId(), winnerId, loserId, isDraw);
		service.updateRatingsAndSaveMatchAndPlayers(match);
		service.deleteChallenge(challenge);

		postToInvolvedChannels(challenge, match, game, hasChallengerReported, isDraw, isWin);
		bot.postToResultChannel(game, match);

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), match);
	}

	private void postToInvolvedChannels(ChallengeModel challenge, Match match, Game game,
										boolean hasChallengerReported, boolean isDraw, boolean isWin) {
		Message reportPresentMessage = hasChallengerReported ?
				bot.getChallengerMessage(challenge).block()
				: bot.getAcceptorMessage(challenge).block();
		String reportPresentOldRating = service.formatRating(isDraw ?
				hasChallengerReported ? match.getWinnerOldRating() : match.getLoserOldRating()
				: isWin ? match.getWinnerOldRating() : match.getLoserOldRating());
		String reportPresentNewRating = service.formatRating(isDraw ?
				hasChallengerReported ? match.getWinnerNewRating() : match.getLoserNewRating()
				: isWin ? match.getWinnerNewRating() : match.getLoserNewRating());
		new MessageUpdater(reportPresentMessage)
				.makeAllNotBold()
				.addLine(String.format("Your opponent has failed to report within %s minutes. " +
								"The match is getting resolved according to your report now.",
						game.getMatchAutoResolveTime()))
				.addLine(String.format("Your rating went from %s to %s.",
						reportPresentOldRating, reportPresentNewRating))
				.makeAllItalic()
				.resend()
				.withComponents(none).subscribe();

		Message reportAbsentMessage = hasChallengerReported ?
				bot.getAcceptorMessage(challenge).block()
				: bot.getChallengerMessage(challenge).block();
		String reportAbsentOldRating = service.formatRating(isDraw ?
				hasChallengerReported ? match.getLoserOldRating() : match.getWinnerOldRating()
				: isWin ? match.getLoserOldRating() : match.getWinnerOldRating());
		String reportAbsentNewRating = service.formatRating(isDraw ?
				hasChallengerReported ? match.getLoserNewRating() : match.getWinnerNewRating()
				: isWin ? match.getLoserNewRating() : match.getWinnerNewRating());
		new MessageUpdater(reportAbsentMessage)
				.makeAllNotBold()
				.addLine(String.format("You have failed to report within %s minutes. " +
								"The match is getting resolved according to your opponent's report now.",
						game.getMatchAutoResolveTime()))
				.addLine(String.format("Your rating went from %s to %s.",
						reportAbsentOldRating, reportAbsentNewRating))
				.makeAllItalic()
				.resend()
				.withComponents(none).subscribe();
	}

	private void autoResolveMatchAsCancel(ChallengeModel challenge, boolean isReportedByChallenger) {
		Game game = service.findGameByGuildId(challenge.getGuildId()).get();
		service.deleteChallenge(challenge);

		reportPresentMessage = isReportedByChallenger ?
				bot.getChallengerMessage(challenge).block()
				: bot.getAcceptorMessage(challenge).block();
		new MessageUpdater(reportPresentMessage)
				.makeAllNotBold()
				.addLine("Your opponent has failed to report within %s minutes. " +
						"The match is canceled.")
				.makeAllItalic()
				.resend()
				.withComponents(none).subscribe();
		reportAbsentMessage = isReportedByChallenger ?
				bot.getAcceptorMessage(challenge).block()
				: bot.getChallengerMessage(challenge).block();
		new MessageUpdater(reportAbsentMessage)
				.makeAllNotBold()
				.addLine("You have failed to report within %s minutes. " +
						"The match is canceled.")
				.makeAllItalic()
				.resend()
				.withComponents(none).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), null);
	}
}
