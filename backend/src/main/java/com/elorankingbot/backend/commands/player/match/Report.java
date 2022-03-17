package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.UUID;

import static com.elorankingbot.backend.model.Match.ReportIntegrity.*;
import static com.elorankingbot.backend.tools.FormatTools.formatRating;

public abstract class Report extends ButtonCommandRelatedToMatch {

	private final ReportStatus reportStatus;

	public Report(ButtonInteractionEvent event, Services services, ReportStatus reportStatus) {
		super(event, services);
		this.reportStatus = reportStatus;
	}

	public void execute() {
		if (match.isDispute()) {
			event.acknowledge().subscribe();
			return;
		}

		match.reportAndSetConflictData(activePlayerId, reportStatus);
		Match.ReportIntegrity reportIntegrity = match.getReportIntegrity();

		if (reportIntegrity == INCOMPLETE) {
			processIncompleteReporting();
			dbservice.saveMatch(match);
		}
		if (reportIntegrity == COMPLETE) {
			MatchResult matchResult = MatchService.generateMatchResult(match);
			processMatchResult(matchResult);
			dbservice.saveMatchResult(matchResult);
			dbservice.deleteMatch(match);
		}
		if (reportIntegrity == CONFLICT) {
			processConflict();
			dbservice.saveMatch(match);
		}
		event.acknowledge().subscribe();
	}

	private void processIncompleteReporting() {
		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();

		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						boolean hasPlayerReported = match.getReportStatus(player.getId()) != ReportStatus.NOT_YET_REPORTED;
						System.out.println(hasPlayerReported);
						String embedTitle = EmbedBuilder.makeTitleForIncompleteMatch(match, hasPlayerReported, false);
						EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(embedTitle, match, activeUser.getTag());
						message.edit().withEmbeds(embedCreateSpec).subscribe();
					});
		}


		// TODO!  ...autoresolve bei 1? fehlendem vote, ansonsten dispute
		//timedTaskQueue.addTimedTask(TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE, game.getMatchAutoResolveTime(),
		//		challenge.getId(), 0L, null);
	}

	private void processMatchResult(MatchResult matchResult) {
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
								.createCompletedMatchEmbed(embedTitle, match, matchResult, player.getTag());

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

	private void processConflict() {
		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						boolean hasPlayerReported = match.getReportStatus(player.getId()) != ReportStatus.NOT_YET_REPORTED;
						String embedTitle = EmbedBuilder.makeTitleForIncompleteMatch(match, hasPlayerReported, true);
						ActionRow actionRow = createConflictActionRow(match.getId(), game.isAllowDraw(), hasPlayerReported);
						EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(embedTitle, match, player.getTag());
						message.edit().withEmbeds(embedCreateSpec).withComponents(actionRow).subscribe();
					});
		}
	}

	static ActionRow createConflictActionRow(UUID matchId, boolean allowDraw, boolean hasPlayerReported) {
		if (hasPlayerReported) {
			return ActionRow.of(
					Buttons.redo(matchId),
					Buttons.dispute(matchId));
		} else {
			if (allowDraw) {
				return ActionRow.of(
						Buttons.win(matchId),
						Buttons.lose(matchId),
						Buttons.draw(matchId),
						Buttons.dispute(matchId));
			} else {
				return ActionRow.of(
						Buttons.win(matchId),
						Buttons.lose(matchId),
						Buttons.dispute(matchId));
			}
		}
	}
}