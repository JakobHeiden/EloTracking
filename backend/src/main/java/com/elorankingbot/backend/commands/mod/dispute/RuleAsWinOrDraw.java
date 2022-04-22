package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import static com.elorankingbot.backend.model.ReportStatus.*;
import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.CHANNEL_DELETE;

public abstract class RuleAsWinOrDraw extends ButtonCommandRelatedToDispute {

	protected int winningTeamIndex;
	protected boolean isRuleAsWin;

	public RuleAsWinOrDraw(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!isByModeratorOrAdminDoReply()) return;

		if (isRuleAsWin) {
			for (int i = 0; i < match.getNumTeams(); i++) {
				for (Player player : match.getTeams().get(i)) {
					match.reportAndSetConflictData(player.getId(), i == winningTeamIndex ? WIN : LOSE);
				}
			}
		} else {
			for (Player player : match.getPlayers()) {
				match.reportAndSetConflictData(player.getId(), DRAW);
			}
		}
		MatchResult matchResult = MatchService.generateMatchResult(match);
		matchService.processMatchResult(matchResult, match);
		timedTaskQueue.addTimedTask(CHANNEL_DELETE, 24 * 60, match.getChannelId(), 0L, null);

		if (isRuleAsWin) {
			postToDisputeChannelAndUpdateButtons(String.format("**%s has ruled this match a %s %s for team #%s.**",
					moderatorTag, WIN.asNoun, WIN.asEmojiAsString(), winningTeamIndex + 1));
		} else {
			postToDisputeChannelAndUpdateButtons(String.format("**%s has ruled this match a %s %s.**",
					moderatorTag, DRAW.asNoun, DRAW.asEmojiAsString()));
		}
		event.acknowledge().subscribe();
	}
}
