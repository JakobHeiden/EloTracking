package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.ReportStatus;
import com.elorankingbot.backend.service.EmbedBuilder;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

import java.util.UUID;

import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.CHANNEL_DELETE;

public abstract class Report extends ButtonCommandRelatedToMatch {

	private final ReportStatus reportStatus;

	public Report(ButtonInteractionEvent event, Services services, ReportStatus reportStatus) {
		super(event, services);
		this.reportStatus = reportStatus;
	}

	public void execute() {
		if (!activeUserIsInvolvedInMatch() || match.isDispute()) {
			event.acknowledge().subscribe();
			return;
		}

		match.reportAndSetConflictData(activePlayerId, reportStatus);
		Match.ReportIntegrity reportIntegrity = match.getReportIntegrity();
		switch (reportIntegrity) {
			case INCOMPLETE -> {
				processIncompleteReporting();
				dbService.saveMatch(match);
			}
			case CONFLICT -> {
				processConflictingReporting();
				dbService.saveMatch(match);
			}
			case CANCEL -> {
				matchService.processCancel(match);
			}
			case COMPLETE -> {
				MatchResult matchResult = MatchService.generateMatchResult(match);
				matchService.processMatchResult(matchResult, match);
				timedTaskQueue.addTimedTask(CHANNEL_DELETE, 24 * 60, match.getChannelId(), 0L, null);
			}
		}
		event.acknowledge().subscribe();
	}

	private void processIncompleteReporting() {
		String title = "Not all players have reported yet. " +
				"Please report the result of the match, if you haven't already.";
		bot.getMessage(match.getMessageId(), match.getChannelId()).subscribe(message -> message
				.edit().withEmbeds(EmbedBuilder.createMatchEmbed(title, match))
				.withComponents(MatchService.createActionRow(match)).subscribe());

		// TODO!  ...autoresolve bei 1? fehlendem vote, ansonsten dispute
		//timedTaskQueue.addTimedTask(TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE, game.getMatchAutoResolveTime(),
		//		challenge.getId(), 0L, null);
	}

	private void processConflictingReporting() {
		String title = "There are conflicts. Please try to sort out the issue with the other players. " +
				"If you cannot find a solution, you can file a dispute.";
		bot.getMessage(match.getMessageId(), match.getChannelId()).subscribe(message -> message
				.edit().withEmbeds(EmbedBuilder.createMatchEmbed(title, match))
				.withComponents(createConflictActionRow(match)).subscribe());
	}

	static ActionRow createConflictActionRow(Match match) {
		boolean allowDraw = match.getGame().isAllowDraw();
		UUID matchId = match.getId();
		if (allowDraw) {
			return ActionRow.of(
					Buttons.win(matchId),
					Buttons.lose(matchId),
					Buttons.draw(matchId),
					Buttons.cancel(matchId),
					Buttons.dispute(matchId));
		} else {
			return ActionRow.of(
					Buttons.win(matchId),
					Buttons.lose(matchId),
					Buttons.cancel(matchId),
					Buttons.dispute(matchId));
		}
	}
}