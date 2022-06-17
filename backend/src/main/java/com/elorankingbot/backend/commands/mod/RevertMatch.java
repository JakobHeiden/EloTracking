package com.elorankingbot.backend.commands.mod;

import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.MessageCommand;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.MatchResultReference;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.PlayerGameStats;
import com.elorankingbot.backend.service.EmbedBuilder;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Mono;

import java.util.Optional;

@ModCommand
public class RevertMatch extends MessageCommand {// TODO in help steht UndoMatch, aendern in Undo Match

	private MatchResult matchResult;
	private MatchResultReference matchResultReference;

	public RevertMatch(MessageInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(getCommandName(RevertMatch.class))
				.type(3)
				.build();
	}

	public static String getShortDescription() {
		return "Revert a match result.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"This is not a slash command, but rather a message command. Access it by right-clicking on a match result " +
				"-> Apps -> " + getCommandName(RevertMatch.class) + ".\n" +
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

		matchResult.setReverted();
		dbService.saveMatchResult(matchResult);
		updatePlayers();
		updateMessages();
		bot.updateLeaderboard(matchResult.getGame(), Optional.of(matchResult));
		event.reply("Match reverted.").withEphemeral(true).subscribe();
	}

	private void updatePlayers() {
		matchResult.getAllPlayerMatchResults().forEach(playerMatchResult -> {
			Player player = playerMatchResult.getPlayer();
			PlayerGameStats playerGameStats = player.getOrCreateGameStats(matchResult.getGame());
			double ratingChange = playerMatchResult.getNewRating() - playerMatchResult.getOldRating();
			playerGameStats.setRating(playerGameStats.getRating() - ratingChange);
			playerGameStats.subtractResultStatus(playerMatchResult.getResultStatus());
			dbService.savePlayer(player);
		});
	}

	private void updateMessages() {
		String thisMatchWasUndoneMessage = String.format("**This match has been reverted by %s on %s.**",
				event.getInteraction().getUser().getTag(),
				EmbedBuilder.dateFormat.format(matchResult.getRevertedWhen()));
		bot.getMessage(matchResultReference.getMatchMessageId(), matchResultReference.getMatchChannelId())
				.onErrorResume(e -> Mono.empty())
				.subscribe(matchMessage -> matchMessage.edit()
						.withContent(Possible.of(Optional.of(thisMatchWasUndoneMessage))).subscribe());
		bot.getMessage(matchResultReference.getResultMessageId(), matchResultReference.getResultChannelId())
				.onErrorResume(e -> Mono.empty())
				.subscribe(matchMessage -> matchMessage.edit()
						.withContent(Possible.of(Optional.of(thisMatchWasUndoneMessage))).subscribe());
	}
}
