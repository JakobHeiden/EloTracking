package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.PlayerMatchResult;
import com.elorankingbot.backend.service.RatingCalculations;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.EmbedBuilder;
import com.elorankingbot.backend.tools.Emojis;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

import static com.elorankingbot.backend.model.ReportStatus.*;
import static com.elorankingbot.backend.tools.FormatTools.formatRating;

public class RuleAsWin extends ButtonCommandRelatedToDispute {

	public RuleAsWin(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!isByModeratorOrAdminDoReply()) return;

		int winningTeamIndex = Integer.parseInt(event.getCustomId().split(":")[2]);
		for (int i = 0; i < match.getNumTeams(); i++) {
			for (Player player : match.getTeams().get(i)) {
				match.reportAndSetConflictData(player.getId(), i == winningTeamIndex ? WIN : LOSE);
			}
		}
		match.setOrWasConflict(false);// TODO dirty hack! EmbedBuilder neu machen!
		MatchResult matchResult = RatingCalculations.generateMatchResult(match);
		updatePlayerMessages(matchResult);
		dbservice.saveMatchResult(matchResult);
		dbservice.deleteMatch(match);
		postToDisputeChannelAndUpdateButtons(String.format(
				"**%s has ruled this match a win %s for team #%s.**",// TODO! hier auch tags, oder nur tags
				moderatorName, Emojis.win.asUnicodeEmoji().get().getRaw(), winningTeamIndex + 1));
		bot.postToResultChannel(matchResult);
		bot.refreshLeaderboard(server);


		// TODO! channels closen, sind die pings sinnvoll? was ist mit mentions?

		//addMatchSummarizeToQueue(matchResult);
		event.acknowledge().subscribe();
	}

	private void updatePlayerMessages(MatchResult matchResult) {
		for (Player player : match.getPlayers()) {
			bot.getPlayerMessage(player, match)
					.subscribe(message -> {
						PlayerMatchResult playerMatchResult = matchResult.getPlayerMatchResult(player.getId());
						String embedTitle = String.format("%s has ruled the match your %s %s. Your new rating: %s (%s)",
								moderatorName,
								playerMatchResult.getResultStatus().asNoun(),
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
	}
}
