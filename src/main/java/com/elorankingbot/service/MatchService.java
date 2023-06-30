package com.elorankingbot.service;

import com.elorankingbot.components.EmbedBuilder;
import com.elorankingbot.model.*;
import com.google.common.collect.Iterables;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class MatchService {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final ChannelManager channelManager;
	private final QueueScheduler queueScheduler;
	private final Consumer<Object> NO_OP = object -> {};

	public MatchService(Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.channelManager = services.channelManager;
		this.queueScheduler = services.queueScheduler;
	}

	public void startMatch(Match match) {
		TextChannel channel = channelManager.createMatchChannel(match).block();
		channelManager.sendMatchMessage(channel, match);
		dbService.saveMatch(match);
	}

	public static MatchResult generateMatchResult(Match match) {
		MatchResult matchResult = new MatchResult(match);
		Game game = match.getQueue().getGame();
		for (List<Player> team : match.getTeams()) {
			List<Player> allOtherPlayers = match.getPlayers();
			team.forEach(allOtherPlayers::remove);
			double averageTeamRating = team.stream()
					.mapToDouble(player -> player.getOrCreatePlayerGameStats(game).getRating())
					.average().getAsDouble();
			double averageOtherRating = allOtherPlayers.stream()
					.mapToDouble(player -> player.getOrCreatePlayerGameStats(game).getRating())
					.average().getAsDouble();
			double numOtherTeams = match.getQueue().getNumTeams() - 1;
			// TODO erwartungswert skaliert bei zb 3 spielern lediglich von 0 bis 2/3. sollte vllt wie im speziellen fall von 0 bis 1?
			double expectedResult = 1 / (numOtherTeams + Math.pow(10, (averageOtherRating - averageTeamRating) / 400));

			TeamMatchResult teamResult = new TeamMatchResult();
			for (Player player : team) {
				double actualResult = match.getReportStatus(player.getId()).value;
				double oldRating = player.getOrCreatePlayerGameStats(game).getRating();
				double newRating = oldRating + match.getQueue().getK() * (actualResult - expectedResult);
				PlayerMatchResult playerMatchResult = new PlayerMatchResult(
						player, player.getTag(),
						ReportStatus.valueOf(match.getReportStatus(player.getId()).name()),
						oldRating, newRating);
				teamResult.add(playerMatchResult);
			}

			matchResult.addTeamMatchResult(teamResult);
		}
		return matchResult;
	}

	public static MatchResult generateCanceledMatchResult(Match match) {
		MatchResult matchResult = new MatchResult(match);
		Game game = match.getQueue().getGame();
		for (List<Player> team : match.getTeams()) {
			TeamMatchResult teamResult = new TeamMatchResult();
			for (Player player : team) {
				double oldRating = player.getOrCreatePlayerGameStats(game).getRating();
				PlayerMatchResult playerMatchResult = new PlayerMatchResult(
						player, player.getTag(),
						ReportStatus.CANCEL,
						oldRating, oldRating);
				teamResult.add(playerMatchResult);
			}
			matchResult.addTeamMatchResult(teamResult);
		}
		return matchResult;
	}

	public void processMatchResult(MatchResult matchResult, Match match, String embedTitle, Function<Role, Consumer<Throwable>> manageRoleFailedCallback) {
		Game game = match.getGame();
		TextChannel matchChannel = (TextChannel) bot.getChannelById(match.getChannelId()).block();// TODO was wenn der channel weg ist
		Message newMatchMessage = matchChannel.createMessage(EmbedBuilder.createCompletedMatchEmbed(embedTitle, matchResult))
				// users have called for these mentions to be removed. if other users again call for them be implemented,
				// make them a setting:
				//.withContent(match.getAllMentions())
				.block();
		newMatchMessage.pin().subscribe();
		bot.getMessage(match.getMessageId(), match.getChannelId())
				.subscribe(oldMatchMessage -> oldMatchMessage.delete().subscribe());
		channelManager.moveToArchive(game.getServer(), matchChannel);

		Message resultChannelMessage = channelManager.postToResultChannel(matchResult);

		dbService.saveMatchResultReference(new MatchResultReference(resultChannelMessage, newMatchMessage, matchResult.getId()));
		dbService.saveMatchResult(matchResult);
		dbService.deleteMatch(match);
		matchResult.getPlayers().forEach(player -> {
			player.addMatchResult(matchResult);
			dbService.savePlayer(player);
		});
		if (matchResult.isCanceled()) {
			return;
		}

		matchResult.getPlayers().forEach(player -> {
			queueScheduler.updatePlayerInAllQueuesOfGame(game, player);
			updatePlayerMatches(game, player);
		});
		boolean leaderboardNeedsRefresh = dbService.updateRankingsEntries(matchResult);
		if (leaderboardNeedsRefresh) channelManager.refreshLeaderboard(game);
		for (Player player : match.getPlayers()) {
			updatePlayerRank(game, player, manageRoleFailedCallback);
		}
		dbService.addMatchResultToStats(matchResult);
	}

	/* This differs from processMatchResult in the following ways:
	- there is no Match, goes directly to MatchResult
	- there is no matchChannel, players are informed in DMs
	- event is supplied for the callback if sending DM fails */
	public EmbedCreateSpec processForcedMatchResult(MatchResult forcedMatchResult, List<User> users, String embedTitle,
													ChatInputInteractionEvent event, Function<Role, Consumer<Throwable>> manageRoleFailedCallback) {
		EmbedCreateSpec matchEmbed = EmbedBuilder.createCompletedMatchEmbed(embedTitle, forcedMatchResult);
		for (User user : users) {
			bot.sendDM(user, event, matchEmbed);
		}
		Message resultChannelMessage = channelManager.postToResultChannel(forcedMatchResult);
		dbService.saveMatchResultReference(new MatchResultReference(resultChannelMessage, forcedMatchResult.getId()));
		dbService.saveMatchResult(forcedMatchResult);
		Game game = forcedMatchResult.getGame();
		forcedMatchResult.getPlayers().forEach(player -> {
			player.addMatchResult(forcedMatchResult);
			dbService.savePlayer(player);
			queueScheduler.updatePlayerInAllQueuesOfGame(game, player);
			updatePlayerMatches(game, player);
			updatePlayerRank(game, player, manageRoleFailedCallback);
		});
		boolean leaderboardNeedsRefresh = dbService.updateRankingsEntries(forcedMatchResult);
		if (leaderboardNeedsRefresh) channelManager.refreshLeaderboard(game);
		dbService.addMatchResultToStats(forcedMatchResult);
		return matchEmbed;
	}

	public void updatePlayerMatches(Game game, Player player) {
		for (Match match : dbService.findAllMatchesByServer(game.getServer())) {
			boolean hasMatchChanged = match.updatePlayerIfPresent(player);
			if (hasMatchChanged) {
				dbService.saveMatch(match);
				bot.getMessage(match.getMessageId(), match.getChannelId())
						.subscribe(message -> message.edit()
										.withEmbeds(EmbedBuilder.createMatchEmbed(message.getEmbeds().get(0).getTitle().get(), match))
										.subscribe(),
								throwable -> {});
			}
		}
	}

	public void updatePlayerRank(Game game, Player player, Function<Role, Consumer<Throwable>> manageRoleFailedCallback) {
		List<Integer> applicableRequiredRatings = new ArrayList<>(game.getRequiredRatingToRankId().keySet().stream()
				.filter(requiredRating -> player.hasPlayerGameStats(game)
						&& player.getOrCreatePlayerGameStats(game).getRating() > requiredRating)
				.toList());
		if (applicableRequiredRatings.size() == 0) return;

		Collections.sort(applicableRequiredRatings);
		int relevantRequiredRating = Iterables.getLast(applicableRequiredRatings);
		Role highestApplicableRankRole = bot.getRole(game.getServer(), game.getRequiredRatingToRankId().get(relevantRequiredRating));

		Member member = bot.getMember(player);
		List<Role> currentRankRoles = member.getRoles()
				.filter(role -> game.getRequiredRatingToRankId().containsValue(role.getId().asLong()))
				.collectList().block();
		if (!currentRankRoles.contains(highestApplicableRankRole)) {
			member.addRole(highestApplicableRankRole.getId()).subscribe(NO_OP, manageRoleFailedCallback.apply(highestApplicableRankRole));
		}
		currentRankRoles.stream().filter(role -> !role.equals(highestApplicableRankRole))
				.forEach(role -> member.removeRole(role.getId()).subscribe(NO_OP, manageRoleFailedCallback.apply(role)));
	}
}
