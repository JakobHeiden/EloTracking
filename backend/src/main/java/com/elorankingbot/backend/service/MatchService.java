package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.*;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class MatchService {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final ChannelManager channelManager;
	private final QueueService queueService;

	public MatchService(Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.channelManager = services.channelManager;
		this.queueService = services.queueService;
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
					.mapToDouble(player -> player.getOrCreateGameStats(game).getRating())
					.average().getAsDouble();
			double averageOtherRating = allOtherPlayers.stream()
					.mapToDouble(player -> player.getOrCreateGameStats(game).getRating())
					.average().getAsDouble();
			double numOtherTeams = match.getQueue().getNumTeams() - 1;
			// TODO erwartungswert skaliert bei zb 3 spielern lediglich von 0 bis 2/3. sollte vllt wie im speziellen fall von 0 bis 1?
			double expectedResult = 1 / (numOtherTeams + Math.pow(10, (averageOtherRating - averageTeamRating) / 400));

			TeamMatchResult teamResult = new TeamMatchResult();
			for (Player player : team) {
				double actualResult = match.getReportStatus(player.getId()).value;
				double oldRating = player.getOrCreateGameStats(game).getRating();
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
				double oldRating = player.getOrCreateGameStats(game).getRating();
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

	public void processMatchResult(MatchResult matchResult, Match match, String embedTitle) {
		TextChannel matchChannel = (TextChannel) bot.getChannelById(match.getChannelId()).block();// TODO was wenn der channel weg ist
		Game game = match.getGame();

		Message newMatchMessage = matchChannel.createMessage(EmbedBuilder.createCompletedMatchEmbed(embedTitle, matchResult))
				.withContent(match.getAllMentions()).block();
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
		if (matchResult.getAllPlayerMatchResults().get(0).getResultStatus().equals(ReportStatus.CANCEL)) {
			return;
		}

		matchResult.getPlayers().forEach(player -> {
			queueService.updatePlayerInAllQueuesOfGame(game, player);
			updatePlayerMatches(game, player);
		});
		boolean leaderboardNeedsRefresh = dbService.updateRankingsEntries(matchResult);
		if (leaderboardNeedsRefresh) channelManager.refreshLeaderboard(game);
		for (Player player : match.getPlayers()) {
			bot.updatePlayerRank(game, player);
		}
		dbService.addMatchResultToStats(matchResult);
	}

	// TODO processMatchResult und die updates in channels und messages evtl trennen
	// TODO das ist ungluecklich, dass es 2 methoden gibt, die MatchResults prozessieren.
	public EmbedCreateSpec processForcedMatchResult(MatchResult forcedMatchResult, List<User> users, String embedTitle) {
		EmbedCreateSpec matchEmbed = EmbedBuilder.createCompletedMatchEmbed(embedTitle, forcedMatchResult);
		for (User user : users) {
			user.getPrivateChannel().subscribe(privateChannel -> privateChannel.createMessage(matchEmbed)
					.onErrorResume(e -> Mono.empty()).subscribe());
		}
		Message resultChannelMessage = channelManager.postToResultChannel(forcedMatchResult);
		dbService.saveMatchResultReference(new MatchResultReference(resultChannelMessage, forcedMatchResult.getId()));
		dbService.saveMatchResult(forcedMatchResult);
		Game game = forcedMatchResult.getGame();
		forcedMatchResult.getPlayers().forEach(player -> {
			player.addMatchResult(forcedMatchResult);
			dbService.savePlayer(player);
			queueService.updatePlayerInAllQueuesOfGame(game, player);
			updatePlayerMatches(game, player);
			bot.updatePlayerRank(game, player);
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
				bot.getMessage(match.getMessageId(), match.getChannelId()).subscribe(message -> message
						.edit().withEmbeds(EmbedBuilder.createMatchEmbed(message.getEmbeds().get(0).getTitle().get(), match))
						.subscribe());
			}
		}
	}
}
