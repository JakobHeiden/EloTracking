package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchService {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final QueueService queueService;
	private static int k = 16;// TODO

	public MatchService(Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.queueService = services.queueService;
	}

	public static MatchResult generateMatchResult(Match match) {
		MatchResult matchResult = new MatchResult(match);
		Game game = match.getQueue().getGame();
		for (List<Player> team : match.getTeams()) {
			List<Player> otherPlayers = match.getPlayers();
			team.forEach(otherPlayers::remove);
			double averageTeamRating = team.stream()
					.mapToDouble(pl -> pl.getGameStats(game).getRating())
					.average().getAsDouble();
			double averageOtherRating = otherPlayers.stream()
					.mapToDouble(pl -> pl.getGameStats(game).getRating())
					.average().getAsDouble();
			double numOtherTeams = match.getQueue().getNumTeams() - 1;
			double expectedResult = 1 / (numOtherTeams * (1 + Math.pow(10, (averageOtherRating - averageTeamRating) / 400)));

			TeamMatchResult teamResult = new TeamMatchResult();
			for (Player player : team) {
				double actualResult = match.getReportStatus(player.getId()).getValue();
				double oldRating = player.getGameStats(game).getRating();
				double newRating = oldRating + k * (actualResult - expectedResult);
				PlayerMatchResult playerMatchResult = new PlayerMatchResult(matchResult,
						player, player.getTag(),
						ReportStatus.valueOf(match.getReportStatus(player.getId()).name()),
						oldRating, newRating);
				teamResult.add(playerMatchResult);
			}

			matchResult.addTeamMatchResult(teamResult);
		}
		return matchResult;
	}

	public void startMatch(Match match, List<User> usersAlreadyGathered) {
		for (Player player : match.getPlayers()) {
			queueService.removePlayerFromAllQueues(match.getServer(), player);
		}
		List<User> users = gatherAllUsers(match, usersAlreadyGathered);
		sendPlayerMessages(match, users);
		dbService.saveMatch(match);
	}

	private List<User> gatherAllUsers(Match match, List<User> usersAlreadyGathered) {
		List<Player> players = match.getPlayers();
		List<User> allUsers = new ArrayList<>(usersAlreadyGathered);
		Set<Long> userIdsAlreadyGathered = usersAlreadyGathered.stream().map(user -> user.getId().asLong()).collect(Collectors.toSet());
		Set<Long> allUserIds = players.stream().map(Player::getUserId).collect(Collectors.toSet());
		List<Mono<User>> userMonos = new ArrayList<>(allUserIds.size() - userIdsAlreadyGathered.size());
		for (long userId : allUserIds) {
			if (!userIdsAlreadyGathered.contains(userId)) {
				Mono<User> userMono = bot.getUser(userId);
				userMonos.add(userMono);
				userMono.subscribe(allUsers::add);
			}
		}
		Mono.when(userMonos).block();
		return allUsers;
	}

	private void sendPlayerMessages(Match match, List<User> users) {
		List<Mono<PrivateChannel>> channelMonos = new ArrayList<>(users.size());
		for (User user : users) {
			EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(
					EmbedBuilder.makeTitleForIncompleteMatch(match, false, false),
					match, user.getTag());
			channelMonos.add(bot.getPrivateChannelByUserId(user.getId().asLong())
					.doOnNext(privateChannel -> {
						Message message = privateChannel.createMessage(embedCreateSpec)
								.withComponents(createActionRow(match.getId(), match.getGame().isAllowDraw())).block();
						UUID playerId = Player.generateId(match.getGame().getGuildId(), user.getId().asLong());
						match.getPlayerIdToMessageId().put(playerId, message.getId().asLong());
						match.getPlayerIdToPrivateChannelId().put(playerId, privateChannel.getId().asLong());
					}));
		}
		Mono.when(channelMonos).block();
	}

	private static ActionRow createActionRow(UUID matchId, boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.draw(matchId),
				Buttons.cancel(matchId));
		else return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.cancel(matchId));
	}

	public static String formatRating(double rating) {
		return String.format("%.1f", Float.valueOf(Math.round(rating * 10)) / 10);
	}
}
