package com.elorankingbot.commands.player;

import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.command.annotations.PlayerCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.components.EmbedBuilder;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.MatchResult;
import com.elorankingbot.model.Player;
import com.elorankingbot.model.RankingsExcerpt;
import com.elorankingbot.service.Services;
import com.elorankingbot.timedtask.DurationParser;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionReplyEditMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static discord4j.core.object.command.ApplicationCommandOption.Type.USER;

@PlayerCommand
@GlobalCommand
public class PlayerInfo extends SlashCommand {

	private Player targetPlayer;
	private boolean isSelfInfo;

	public PlayerInfo(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(PlayerInfo.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.addOption(ApplicationCommandOptionData.builder()
						.name("player")
						.description("The player to get information on. Omit to get information on yourself.")
						.type(USER.getValue())
						.required(false)
						.build())
				.build();
	}

	public static String getShortDescription() {
		return "Get information on a player.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Optional:` `player` The player to get information about. If this is omitted, you will get information " +
				"about yourself, visible to only yourself. Otherwise you will get openly visible information.";
	}

	protected void execute() {
		User targetUser;
		if (event.getOption("player").isEmpty()) {
			targetUser = event.getInteraction().getUser();
			isSelfInfo = true;
		} else {
			targetUser = event.getOption("player").get().getValue().get().asUser().block();
			isSelfInfo = false;
		}

		if (targetUser.isBot()) {
			event.reply("Bots are not players.").subscribe();
			return;
		}
		Optional<Player> maybeTargetPlayer = dbService.findPlayerByGuildIdAndUserId(guildId, targetUser.getId().asLong());
		if (maybeTargetPlayer.isEmpty()) {
			event.reply("No player data found. That user has not yet used the bot.").subscribe();
			return;
		}

		event.deferReply().withEphemeral(isSelfInfo).subscribe();

		targetPlayer = maybeTargetPlayer.get();
		List<EmbedCreateSpec> embeds = new ArrayList<>();
		for (String gameName : targetPlayer.getGameNameToPlayerGameStats().keySet()) {
			Game game = server.getGame(gameName);
			RankingsExcerpt rankingsExcerpt = dbService.getRankingsExcerptForPlayer(game, targetPlayer);
			embeds.add(EmbedBuilder.createRankingsEmbed(rankingsExcerpt));
			embeds.add(EmbedBuilder.createMatchHistoryEmbed(targetPlayer, getMatchHistory(game)));
		}
		String banString = createBanString();

		if (embeds.isEmpty()) {
			event.editReply(banString + "No match data found. That user has likely not yet played a match.").subscribe();
		} else {
			InteractionReplyEditMono reply = targetPlayer.isBanned() ? event.editReply(banString) : event.editReply();
			reply.withEmbeds(embeds).subscribe();
		}
	}

	private String createBanString() {
		String banString = "";
		if (targetPlayer.isBanned()) {
			if (targetPlayer.isPermaBanned()) {
				banString = String.format("**%s banned permanently, or until unbanned.**\n",
						isSelfInfo ? "You are" : "This player is");
			} else {
				int stillBannedMinutes = timedTaskScheduler.getRemainingDuration(targetPlayer.getUnbanAtTimeSlot());
				banString = String.format("**%s still banned for %s.**\n",
						isSelfInfo ? "You are" : "This player is",
						DurationParser.minutesToString(stillBannedMinutes));
			}
		}
		return banString;
	}

	private List<Optional<MatchResult>> getMatchHistory(Game game) {
		List<UUID> fullMatchHistory = targetPlayer.getOrCreatePlayerGameStats(game).getMatchHistory();
		List<UUID> recentMatchHistory = fullMatchHistory.subList(Math.max(0, fullMatchHistory.size() - 20), fullMatchHistory.size());
		return recentMatchHistory.stream().map(dbService::findMatchResult).toList();
	}
}
