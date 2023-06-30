package com.elorankingbot.commands.mod;

import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.command.annotations.ModCommand;
import com.elorankingbot.commands.MessageCommand;
import com.elorankingbot.commands.player.help.HelpComponents;
import com.elorankingbot.components.EmbedBuilder;
import com.elorankingbot.model.MatchResult;
import com.elorankingbot.model.MatchResultReference;
import com.elorankingbot.model.Player;
import com.elorankingbot.model.PlayerGameStats;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import lombok.extern.apachecommons.CommonsLog;
import reactor.core.publisher.Mono;

import java.util.Optional;

@ModCommand
@GlobalCommand
@CommonsLog
public class RevertMatch extends MessageCommand {

	private MatchResult matchResult;
	private MatchResultReference matchResultReference;

	public RevertMatch(MessageInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(HelpComponents.helpEntryNameOf(RevertMatch.class))
				.type(3)
				.build();
	}

	public static String getShortDescription() {
		return "Revert a match result.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"This is not a slash command, but rather a message command. Access it by right-clicking on a match result " +
				"-> Apps -> " + HelpComponents.helpEntryNameOf(RevertMatch.class) + ".\n" +
				"This command does not apply a full rollback with its implications for subsequent matches, but rather simply " +
				"reverts the rating gains/losses as well as the win/loss/draw count of the players.";
	}

	protected void execute() {
		Optional<MatchResultReference> maybeMatchResultReference = dbService.findMatchResultReference(event.getTargetId().asLong());
		if (maybeMatchResultReference.isEmpty()) {
			event.reply("This is not a match.")
					.withEphemeral(true).subscribe();
			return;
		}
		matchResultReference = maybeMatchResultReference.get();
		Optional<MatchResult> maybeMatchResult = dbService.findMatchResult(matchResultReference.getMatchResultId());
		if (maybeMatchResult.isEmpty()) {
			event.reply("I could not find this match.").withEphemeral(true).subscribe();
			return;
		}
		matchResult = maybeMatchResult.get();
		if (matchResult.isReverted()) {
			event.reply("This match already has been reverted.").withEphemeral(true).subscribe();
			return;
		}
		if (matchResult.isCanceled()) {
			event.reply("Cannot revert a match that has been canceled.").withEphemeral(true).subscribe();
			return;
		}

		matchResult.setReverted();
		dbService.saveMatchResult(matchResult);
		updatePlayers();
		updateMessages();
		boolean leaderboardNeedsRefresh = dbService.updateRankingsEntries(matchResult);
		if (leaderboardNeedsRefresh) channelManager.refreshLeaderboard(matchResult.getGame());
		event.reply("Match reverted.").withEphemeral(true).subscribe();
	}

	private void updatePlayers() {
		log.debug("RevertMatch::updatePlayers: " + matchResult.getId());
		matchResult.getAllPlayerMatchResults().forEach(playerMatchResult -> {
			Player player = playerMatchResult.getPlayer();
			PlayerGameStats playerGameStats = player.getOrCreatePlayerGameStats(matchResult.getGame());
			log.debug(String.format("Player %s W%s L%s D%s C%s", player.getTag(),
					playerGameStats.getWins(), playerGameStats.getLosses(), playerGameStats.getDraws(), playerGameStats.getCancels()));
			double ratingChange = playerMatchResult.getNewRating() - playerMatchResult.getOldRating();
			playerGameStats.setRating(playerGameStats.getRating() - ratingChange);
			playerGameStats.subtractResultStatus(playerMatchResult.getResultStatus());
			dbService.savePlayer(player);
			log.debug(String.format("Player %s W%s L%s D%s C%s", player.getTag(),
					playerGameStats.getWins(), playerGameStats.getLosses(), playerGameStats.getDraws(), playerGameStats.getCancels()));
		});
	}

	private void updateMessages() {
		String thisMatchWasUndoneMessage = String.format("**This match has been reverted by %s on %s.**",
				event.getInteraction().getUser().getTag(),
				EmbedBuilder.dateFormat.format(matchResult.getRevertedWhen()));
		if (matchResultReference.getMatchMessageId() != 0L) {
			bot.getMessage(matchResultReference.getMatchMessageId(), matchResultReference.getMatchChannelId())
					.onErrorResume(e -> Mono.empty())
					.subscribe(matchMessage -> matchMessage.edit()
							.withContent(Possible.of(Optional.of(thisMatchWasUndoneMessage))).subscribe());
		}
		bot.getMessage(matchResultReference.getResultMessageId(), matchResultReference.getResultChannelId())
				.onErrorResume(e -> Mono.empty())
				.subscribe(matchMessage -> matchMessage.edit()
						.withContent(Possible.of(Optional.of(thisMatchWasUndoneMessage))).subscribe());
	}
}
