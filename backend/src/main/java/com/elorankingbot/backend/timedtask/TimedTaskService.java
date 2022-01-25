package com.elorankingbot.backend.timedtask;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.http.client.ClientException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TimedTaskService {

	private final EloRankingService service;
	private final DiscordBotService bot;
	private final TimedTaskQueue queue;
	private final GatewayDiscordClient client;

	protected final List none = new ArrayList<>();

	public TimedTaskService(EloRankingService service, @Lazy DiscordBotService bot,
							@Lazy TimedTaskQueue queue, GatewayDiscordClient client) {
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;
	}

	public void markGamesForDeletion() {
		List<Long> allGuildIds = client.getGuilds()
				.map(guild -> guild.getId().asLong())
				.collectList().block();
		service.findAllGames().stream()
				.filter(game -> !allGuildIds.contains(game.getGuildId()))
				.forEach(game -> game.setMarkedForDeletion(true));
	}

	public void deleteGamesMarkedForDeletion() {
		service.findAllGames().stream()
				.filter(game -> game.isMarkedForDeletion())
				.forEach(game -> service.deleteGame(game.getGuildId()));
	}

	public void summarizeMatch(long messageId, long channelId, Object value) {
		Match match = (Match) value;
		Message message = client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block();
		boolean isWinnerMessage = ((PrivateChannel) message.getChannel().block())
				.getRecipientIds().contains(Snowflake.of(match.getWinnerId()));
		System.out.println(isWinnerMessage);
		String opponentName = bot.getPlayerName(isWinnerMessage ? match.getLoserId() : match.getWinnerId());
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block()
				.edit().withContent(String.format("*You played a match against %s and %s. Your rating went from %s to %s.*",
						opponentName,
						match.isDraw() ? "drew :left_right_arrow:" : isWinnerMessage ? "won :arrow_up:" : "lost :arrow_down:",
						isWinnerMessage ? service.formatRating(match.getWinnerOldRating()) : service.formatRating(match.getLoserOldRating()),
						isWinnerMessage ? service.formatRating(match.getWinnerNewRating()) : service.formatRating(match.getLoserNewRating())))
				.subscribe();
	}

	public void deleteMessage(long messageId, long channelId) {
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block().delete().subscribe();
	}

	public void deleteChannel(long channelId) {
		try {
			client.getChannelById(Snowflake.of(channelId)).block().delete().subscribe();
		} catch (ClientException ignored) {
		}
	}

	public void decayOpenChallenge(long challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = service.findChallengeById(challengeId);
		if (maybeChallenge.isEmpty()) return;
		ChallengeModel challenge = maybeChallenge.get();
		if (challenge.isAccepted()) return;

		service.deleteChallengeById(challengeId);
		Optional<Game> maybeGame = service.findGameByGuildId(challenge.getGuildId());
		if (maybeGame.isEmpty()) return;

		new MessageUpdater(challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), client)
				.addLine(String.format("This challenge has expired after not getting accepted within %s minutes.", time))
				.makeAllItalic()
				.update().subscribe();
		new MessageUpdater(challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), client)
				.makeAllNotBold()
				.addLine(String.format("This challenge has expired after not getting accepted within %s minutes.", time))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();

		int timer = service.findGameByGuildId(challenge.getGuildId()).get().getMessageCleanupTime();
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, timer,
				challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, timer,
				challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), null);
	}

	public void decayAcceptedChallenge(long challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = service.findChallengeById(challengeId);
		if (maybeChallenge.isEmpty()) return;

		ChallengeModel challenge = maybeChallenge.get();
		service.deleteChallengeById(challengeId);
		Optional<Game> maybeGame = service.findGameByGuildId(challenge.getGuildId());
		if (maybeGame.isEmpty()) return;

		new MessageUpdater(challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), client)
				.makeAllNotBold()
				.addLine(String.format("This match has expired after not getting reports within %s minutes.", time))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), client)
				.makeAllNotBold()
				.addLine(String.format("This challenge has expired after not getting reports within %s minutes.", time))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();

		int timer = service.findGameByGuildId(challenge.getGuildId()).get().getMessageCleanupTime();
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, timer,
				challenge.getChallengerMessageId(), challenge.getChallengerChannelId(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, timer,
				challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId(), null);
	}

	public void autoResolveMatch(long challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = service.findChallengeById(challengeId);
		if (maybeChallenge.isEmpty()) return;
		ChallengeModel challenge = maybeChallenge.get();
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
		}

		Game game = service.findGameByGuildId(challenge.getGuildId()).get();
		Match match = new Match(challenge.getGuildId(), winnerId, loserId, isDraw);
		service.updateRatingsAndSaveMatch(match);
		service.deleteChallengeById(challenge.getId());
		postToInvolvedChannelsAndAddTimedTask(challenge, match, game, hasChallengerReported, isDraw, isWin);
		bot.postToResultChannel(game, match);
	}

	private void postToInvolvedChannelsAndAddTimedTask(ChallengeModel challenge, Match match, Game game,
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
				.update()
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
				.update()
				.withComponents(none).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), match);
	}

	private void autoResolveMatchAsCancel(ChallengeModel challenge, boolean hasChallengerReported) {
		Game game = service.findGameByGuildId(challenge.getGuildId()).get();
		service.deleteChallenge(challenge);

		Message reportPresentMessage = hasChallengerReported ?
				bot.getChallengerMessage(challenge).block()
				: bot.getAcceptorMessage(challenge).block();
		new MessageUpdater(reportPresentMessage)
				.makeAllNotBold()
				.addLine("Your opponent has failed to report within %s minutes. " +
						"The match is canceled.")
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		Message reportAbsentMessage = hasChallengerReported ?
				bot.getAcceptorMessage(challenge).block()
				: bot.getChallengerMessage(challenge).block();
		new MessageUpdater(reportAbsentMessage)
				.makeAllNotBold()
				.addLine("You have failed to report within %s minutes. " +
						"The match is canceled.")
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), null);
	}
}
