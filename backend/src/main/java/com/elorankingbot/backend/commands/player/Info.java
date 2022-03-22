package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.DurationParser;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

public class Info extends SlashCommand {

	private Player targetPlayer;
	private boolean isSelfInfo;
	private int numTotalPlayers, targetPlayerIndex, numHigherRanksToDisplay;

	public Info(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("info")
				.description("Player information")
				.addOption(ApplicationCommandOptionData.builder()
						.name("player")
						.description("The player to get information about. Only available to moderators. " +
								"Leave empty to get info on self")
						.type(ApplicationCommandOption.Type.USER.getValue())
						.required(false)
						.build())
				.build();
	}

	public void execute() {
		/*
		User targetUser;
		User callingUser = event.getInteraction().getUser();
		if (event.getOption("player").isEmpty()) {
			targetUser = callingUser;
		} else {
			targetUser = event.getOption("player").get().getValue().get().asUser().block();
		}

		isSelfInfo = targetUser == callingUser;
		if (!isSelfInfo) {
			Member callingMember = callingUser.asMember(Snowflake.of(guildId)).block();
			if (!callingMember.getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))
					&& !callingMember.getRoleIds().contains(Snowflake.of(game.getModRoleId()))) {
				event.reply("Only moderators can call this command on others.").subscribe();
				return;
			}
		}
		if (targetUser.isBot()) {
			event.reply("Bots are not players.").subscribe();
			return;
		}
		Optional<Player> maybeTargetPlayer = service.findPlayerByGuildIdAndUserId(guildId, targetUser.getId().asLong());
		if (maybeTargetPlayer.isEmpty()) {
			event.reply("No information available. That user likely has not played a match yet.").subscribe();
			return;
		}
		targetPlayer = maybeTargetPlayer.get();

		EmbedCreateSpec rankingsEmbed = bot.generateLeaderboardEmbed(
				generatePlayerList(), numTotalPlayers, game,
				targetPlayerIndex - numHigherRanksToDisplay + 1, numHigherRanksToDisplay);
		String banString = generateBanString();
		EmbedCreateSpec matchHistoryEmbed = generateMatchHistory(10);

		event.reply().withContent("Information about player " + targetUser.getTag() + banString)
				.withEmbeds(rankingsEmbed, matchHistoryEmbed)
				.withEphemeral(isSelfInfo).subscribe();

		 */
	}

	private List<Player> generatePlayerList() {
		return  null;
		/*
		List<Player> playerList = service.getLeaderboard(guildId);
		numTotalPlayers = playerList.size();
		targetPlayerIndex = playerList.indexOf(targetPlayer);
		numHigherRanksToDisplay = min(10, targetPlayerIndex);
		int numLowerRanksToDisplay = min(10, numTotalPlayers - targetPlayerIndex - 1);
		return playerList.subList(
				targetPlayerIndex - numHigherRanksToDisplay,
				targetPlayerIndex + numLowerRanksToDisplay + 1);

		 */
	}

	private String generateBanString() {
		String banString = "";
		if (targetPlayer.isBanned()) {
			if (targetPlayer.isPermaBanned()) {
				banString = String.format("\n**%s banned permanently, or until unbanned.**",
						isSelfInfo ? "You are" : "This player is");
			} else {
				int stillBannedMinutes = timedTaskQueue.getRemainingDuration(targetPlayer.getUnbanAtTimeSlot());
				banString = String.format("\n**%s still banned for %s.**",
						isSelfInfo ? "You are" : "This player is",
						DurationParser.minutesToString(stillBannedMinutes));
			}
		}
		return banString;
	}

	private EmbedCreateSpec generateMatchHistory(int numMatches) {
		List<MatchResult> matchResultHistory = dbService.getMatchHistory(targetPlayer.getUserId(), guildId);
		if (matchResultHistory.size() > numMatches) matchResultHistory = matchResultHistory.subList(0, numMatches);
		String matchHistoryString = "";
		for (MatchResult matchResult : matchResultHistory) {
			matchHistoryString += generateMatchString(matchResult, targetPlayer.getUserId());
		}
		if (matchHistoryString.equals("")) matchHistoryString = "This player has not played any matches.";

		return EmbedCreateSpec.builder()
				.title(targetPlayer.getTag() + " match history")
				.addField(EmbedCreateFields.Field.of(
						targetPlayer.getTag() + " match history",
						matchHistoryString,
						true))
				.build();
	}

	private String generateMatchString(MatchResult matchResult, long playerId) {
		return null;
		/*
		boolean isWin = match.getWinnerId() == playerId;
		return String.format("%s vs %s: %s -> %s\n",
				match.isDraw() ? ":left_right_arrow:" : isWin ? ":arrow_up:" : ":arrow_down:",
				isWin ? match.getLoserTag() : match.getWinnerTag(),
				formatRating(isWin ? match.getWinnerOldRating() : match.getLoserOldRating()),
				formatRating(isWin ? match.getWinnerNewRating() : match.getLoserNewRating()));

		 */
	}
}
