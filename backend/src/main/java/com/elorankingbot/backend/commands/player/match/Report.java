package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.components.Buttons;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.ReportStatus;
import com.elorankingbot.backend.service.EmbedBuilder;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.DurationParser;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

import java.util.Date;
import java.util.UUID;

import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE;
import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.MATCH_WARN_MISSING_REPORTS;

public abstract class Report extends ButtonCommandRelatedToMatch {

	private final ReportStatus reportStatus;
	private final boolean enforceWaitingPeriods;

	public Report(ButtonInteractionEvent event, Services services, ReportStatus reportStatus) {
		super(event, services);
		this.reportStatus = reportStatus;
		this.enforceWaitingPeriods = services.props.isEnforceWaitingPeriods();
	}

	public void execute() {
		if (!activeUserIsInvolvedInMatch() || match.isDispute()) {
			event.acknowledge().subscribe();
			return;
		}
		long timePassed = new Date().getTime() - match.getTimestamp().getTime();
		if (!this.getClass().equals(Cancel.class) && timePassed < 5*60*1000 && enforceWaitingPeriods) {// TODO
			event.reply(String.format("Please wait another %s before making a report.",
							DurationParser.minutesToString((int) Math.ceil(5 - timePassed / (60*1000)))))
					.withEphemeral(true).subscribe();
			return;
		}

		match.reportAndSetConflictData(activePlayerId, reportStatus);
		Match.ReportIntegrity reportIntegrity = match.getReportIntegrity();
		switch (reportIntegrity) {
			case INCOMPLETE -> {
				processIncompleteReporting();
				if (!match.isHasFirstReport()) {
					timedTaskQueue.addTimedTask(MATCH_WARN_MISSING_REPORTS, 50, 0L, 0L, match.getId());// TODO
					timedTaskQueue.addTimedTask(MATCH_AUTO_RESOLVE, 60, 0L, 0L, match.getId());
					match.setHasFirstReport(true);
				}
				dbService.saveMatch(match);
			}
			case CONFLICT -> {
				processConflictingReporting();
				dbService.saveMatch(match);
			}
			case CANCEL -> {
				matchService.processCancel(match, "The match has been canceled.");
			}
			case COMPLETE -> {
				MatchResult matchResult = MatchService.generateMatchResult(match);
				String resolveMessage = "The match has been resolved. Below are your new ratings and the rating changes.";
				matchService.processMatchResult(matchResult, match, resolveMessage);
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