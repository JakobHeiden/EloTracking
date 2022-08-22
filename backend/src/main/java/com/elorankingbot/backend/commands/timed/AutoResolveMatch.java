package com.elorankingbot.backend.commands.timed;

import com.elorankingbot.backend.commands.player.match.Dispute;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.*;
import com.elorankingbot.backend.timedtask.DurationParser;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.elorankingbot.backend.model.ReportStatus.*;

public class AutoResolveMatch {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final ChannelManager channelManager;
	private final MatchService matchService;
	private final int duration;
	private final UUID matchId;
	private Match match;
	private TextChannel matchChannel, disputeChannel;

	public AutoResolveMatch(Services services, UUID matchId, int duration) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.channelManager = services.channelManager;
		this.matchService = services.matchService;
		this.duration = duration;
		this.matchId = matchId;
	}

	public void execute() {
		Optional<Match> maybeMatch = dbService.findMatch(matchId);
		if (maybeMatch.isEmpty()) return;
		match = maybeMatch.get();
		if (match.isDispute()) return;

		if (match.getReportIntegrity().equals(Match.ReportIntegrity.CONFLICT)) {
			processDispute();
			return;
		}

		String autoresolveMessage = "As 60 minutes have passed since the first report, I have auto-resolved the match.";// TODO
		// if ReportIntegrity != CONFLICT, the possible states of the reporting are greatly reduced
		if (match.getPlayerIdToReportStatus().containsValue(CANCEL)) {
			MatchResult canceledMatchResult = MatchService.generateCanceledMatchResult(match);
			matchService.processMatchResult(canceledMatchResult, match, autoresolveMessage);
			return;
		} else if (match.getPlayerIdToReportStatus().containsValue(DRAW)) {
			match.getPlayers().forEach(player -> match.reportAndSetConflictData(player.getId(), DRAW));
		} else if (match.getPlayerIdToReportStatus().containsValue(WIN)) {
			match.getTeams().forEach(team -> {
				boolean thisTeamReportedWin = team.stream()
						.anyMatch(player -> match.getReportStatus(player.getId()).equals(WIN));
				if (thisTeamReportedWin) {
					team.forEach(player -> match.reportAndSetConflictData(player.getId(), WIN));
				} else {
					team.forEach(player -> match.reportAndSetConflictData(player.getId(), LOSE));
				}
			});
		} else {
			List<List<Player>> teamsReportedLose = match.getTeams().stream()
					.filter(team -> team.stream().anyMatch(player -> match.getReportStatus(player.getId()).equals(LOSE)))
					.toList();
			if (teamsReportedLose.size() == match.getTeams().size() - 1) {
				teamsReportedLose.forEach(team ->
						team.forEach(player -> match.reportAndSetConflictData(player.getId(), LOSE)));
				match.getPlayers().stream()
						.filter(player -> match.getReportStatus(player.getId()).equals(NOT_YET_REPORTED))
						.forEach(player -> match.reportAndSetConflictData(player.getId(), WIN));
			} else {
				processDispute();
				return;
			}
		}
		MatchResult matchResult = MatchService.generateMatchResult(match);
		matchService.processMatchResult(matchResult, match, autoresolveMessage);
	}

	private void processDispute() {
		match.setDispute(true);
		dbService.saveMatch(match);
		matchChannel = (TextChannel) bot.getChannelById(match.getChannelId()).block();
		disputeChannel = channelManager.createDisputeChannel(match).block();
		sendDisputeLinkMessage();
		createDisputeMessage();
	}

	private void sendDisputeLinkMessage() {
		EmbedCreateSpec embed = EmbedCreateSpec.builder()
				.title(String.format("%s have passed since the first report, and this match is due for auto-resolution. " +
								"However, as there are conflicts in the reporting, I opened a dispute. " +
								"For resolution please follow the link:",
						DurationParser.minutesToString(duration)))
				.description(disputeChannel.getMention()).build();
		matchChannel.createMessage(embed).subscribe(message -> message.pin().subscribe());
	}

	private void createDisputeMessage() {
		String embedTitle = "The reporting at the moment the dispute was filed:";
		EmbedCreateSpec embed = EmbedBuilder.createMatchEmbed(embedTitle, match);
		disputeChannel.createMessage(String.format("""
								Welcome %s. Since this match could not be auto-resolved, I created this dispute.
								Only <@&%s> and affected players can view this channel.
								Please state your side of the conflict so a moderator can resolve it.
								The original match channel can be found here: <#%s>
								Note that the Buttons on this message can only be used by moderators.
								""",
						match.getAllMentions(),
						match.getServer().getModRoleId(),
						match.getChannelId()))
				.withEmbeds(embed)
				.withComponents(Dispute.createActionRow(match))
				.subscribe();
	}

		/*

		boolean hasChallengerReported = challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED;
		ChallengeModel.ReportStatus report = hasChallengerReported ?
				challenge.getChallengerReported()
				: challenge.getAcceptorReported();
		long challengerId = challenge.getChallengerUserId();
		long acceptorId = challenge.getAcceptorUserId();
		String challengerTag = challenge.getChallengerTag();
		String acceptorTag = challenge.getAcceptorTag();
		long winnerId = 0L;
		long loserId = 0L;
		String winnerTag = null;
		String loserTag = null;
		boolean isDraw = false;
		boolean isWin = false;
		switch (report) {
			case WIN:
				winnerId = hasChallengerReported ? challengerId : acceptorId;
				loserId = hasChallengerReported ? acceptorId : challengerId;
				winnerTag = hasChallengerReported ? challengerTag : acceptorTag;
				loserTag = hasChallengerReported ? acceptorTag : challengerTag;
				isWin = true;
				break;
			case LOSE:
				winnerId = hasChallengerReported ? acceptorId : challengerId;
				loserId = hasChallengerReported ? challengerId : acceptorId;
				winnerTag = hasChallengerReported ? acceptorTag : challengerTag;
				loserTag = hasChallengerReported ? challengerTag : acceptorTag;
				break;
			case DRAW:
				winnerId = challengerId;
				loserId = acceptorId;
				winnerTag = challengerTag;
				loserTag = acceptorTag;
				isDraw = true;
				break;
			case CANCEL:
				autoResolveMatchAsCancel(challenge, hasChallengerReported);
				return;
		}


		Game game = service.findGameByGuildId(challenge.getGuildId()).get();
		service.addNewPlayerIfPlayerNotPresent(challenge.getGuildId(), challenge.getChallengerUserId());
		service.addNewPlayerIfPlayerNotPresent(challenge.getGuildId(), challenge.getAcceptorUserId());
		Match match = new Match(challenge.getGuildId(), winnerId, loserId, winnerTag, loserTag, isDraw);
		service.updateRatingsAndSaveMatchAndPlayers(match);
		service.deleteChallenge(challenge);

		postToInvolvedChannels(challenge, match, game, hasChallengerReported, isDraw, isWin);
		bot.postToResultChannel(game, match);

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), match);

		 */

	private void postToInvolvedChannels(ChallengeModel challenge, MatchResult matchResult, Game game,
										boolean hasChallengerReported, boolean isDraw, boolean isWin) {
		/*
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

		 */
	}

	private void autoResolveMatchAsCancel(ChallengeModel challenge, boolean isReportedByChallenger) {
		/*
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

		 */
	}
}
