package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.RatingCalculations;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;

import static com.elorankingbot.backend.model.Match.ReportIntegrity.*;
import static com.elorankingbot.backend.tools.FormatTools.formatRating;

public abstract class Report extends ButtonCommandRelatedToMatch {

	private final ReportStatus reportStatus;

	public Report(ButtonInteractionEvent event, Services services, ReportStatus reportStatus) {
		super(event, services);
		this.reportStatus = reportStatus;
	}

	public void execute() {
		match.report(activePlayerId, reportStatus);
		Match.ReportIntegrity reportIntegrity = match.getReportIntegrity();

		if (reportIntegrity == INCOMPLETE) {
			processIncompleteReporting();
			dbservice.saveMatch(match);
		}
		if (reportIntegrity == COMPLETE) {
			MatchResult matchResult = RatingCalculations.generateMatchResult(match);
			processCompleteReporting(matchResult);
			dbservice.saveMatchResult(matchResult);
			dbservice.deleteMatch(match);
		}
		if (reportIntegrity == CONFLICT) processConflictingReporting();
		event.acknowledge().subscribe();
	}

	private void processIncompleteReporting() {
		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						if (player.getUserId() == activeUserId) {
							String activeEmbedTitle = "Not all players have reported yet.";
							EmbedCreateSpec activeEmbedCreateSpec = EmbedBuilder.createMatchEmbed(activeEmbedTitle, match, activeUser.getTag());
							getActiveMessage().block().edit().withEmbeds(activeEmbedCreateSpec).withComponents(none).block();
						} else {
							String embedTitle = message.getEmbeds().get(0).getTitle().get();
							EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(embedTitle, match, activeUser.getTag());
							message.edit().withEmbeds(embedCreateSpec).subscribe();
						}
					});
		}


		// TODO!  ...autoresolve bei 1? fehlendem vote, ansonsten dispute
		//timedTaskQueue.addTimedTask(TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE, game.getMatchAutoResolveTime(),
		//		challenge.getId(), 0L, null);
	}

	private void processCompleteReporting(MatchResult matchResult) {
		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						PlayerMatchResult playerMatchResult = matchResult.getPlayerMatchResult(player.getId());
						String embedTitle = String.format("%s %s %s the match. Your new rating: %s (%s)",
								queue.getNumPlayersPerTeam() == 1 ? "You" : "Your team",
								playerMatchResult.getResultStatus().asVerb(),
								playerMatchResult.getResultStatus().getEmojiAsString(),
								formatRating(playerMatchResult.getNewRating()),
								playerMatchResult.getRatingChangeAsString());
						EmbedCreateSpec embedCreateSpec = EmbedBuilder
								.createCompletedMatchEmbed(embedTitle, match, matchResult, activeUser.getTag());

						message.delete().subscribe();
						bot.getPrivateChannelByUserId(player.getUserId()).subscribe(channel ->
								channel.createMessage(embedCreateSpec).subscribe());
					});
		}

		bot.postToResultChannel(matchResult);
		boolean hasLeaderboardChanged = dbservice.updateAndPersistRankingsAndPlayers(matchResult);
		if (hasLeaderboardChanged) bot.refreshLeaderboard(server);

		/*queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),// TODO verallgemeinern
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), match);
		 */
	}

	private void processConflictingReporting() {
		List<Player> conflictingReports = match.getConflictingReports();
		/*
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, or file a dispute.")
				.makeLastLineBold()
				.update()
				.withComponents(createActionRow(challenge.getId())).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent reported a win :arrow_up:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold()
				.resend()
				.withComponents(createActionRow(challenge.getId()))
				.subscribe(super::updateAndSaveChallenge);

		 */
	}

	static ActionRow createActionRow(long challengeId) {
		return null;
		/*
		return ActionRow.of(
				Buttons.redo(challengeId),
				Buttons.cancelOnConflict(challengeId),
				Buttons.redoOrCancelOnConflict(challengeId),
				Buttons.dispute(challengeId));

		 */
	}
}
