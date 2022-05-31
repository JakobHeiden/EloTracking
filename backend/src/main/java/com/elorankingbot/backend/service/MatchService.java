package com.elorankingbot.backend.service;

import com.elorankingbot.backend.components.Buttons;
import com.elorankingbot.backend.model.*;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

	public void startMatch(Match match) {
		TextChannel channel = bot.createMatchChannel(match).block();
		sendMatchMessage(channel, match);
		dbService.saveMatch(match);
	}

	public static MatchResult generateMatchResult(Match match) {
		MatchResult matchResult = new MatchResult(match);
		Game game = match.getQueue().getGame();
		for (List<Player> team : match.getTeams()) {
			List<Player> otherPlayers = match.getPlayers();
			team.forEach(otherPlayers::remove);
			double averageTeamRating = team.stream()
					.mapToDouble(pl -> pl.getOrCreateGameStats(game).getRating())
					.average().getAsDouble();
			double averageOtherRating = otherPlayers.stream()
					.mapToDouble(pl -> pl.getOrCreateGameStats(game).getRating())
					.average().getAsDouble();
			double numOtherTeams = match.getQueue().getNumTeams() - 1;
			// TODO erwartungswert skaliert bei zb 3 spielern lediglich von 0 bis 2/3. sollte vllt wie im speziellen fall von 0 bis 1?
			double expectedResult = 1 / (numOtherTeams + Math.pow(10, (averageOtherRating - averageTeamRating) / 400));

			TeamMatchResult teamResult = new TeamMatchResult();
			for (Player player : team) {
				double actualResult = match.getReportStatus(player.getId()).value;
				double oldRating = player.getOrCreateGameStats(game).getRating();
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

	private void sendMatchMessage(TextChannel channel, Match match) {
		String title = String.format("Your match of %s is starting. " +
						"I removed you from all other queues you joined on this server, if any. " +
						"Please play the match and come back to report the result afterwards.",
				match.getQueue().getFullName());
		EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(title, match);
		Message message = channel.createMessage(match.getAllMentions())
				.withEmbeds(embedCreateSpec)
				.withComponents(createActionRow(match)).block();
		message.pin().subscribe();
		match.setMessageId(message.getId().asLong());
		match.setChannelId(message.getChannelId().asLong());
	}

	public void processMatchResult(MatchResult matchResult, Match match, String embedTitle) {
		TextChannel channel = (TextChannel) bot.getChannelById(match.getChannelId()).block();// TODO was wenn der channel weg ist
		Game game = match.getGame();

		bot.getMessage(match.getMessageId(), match.getChannelId())
				.subscribe(message -> {
					channel.createMessage(EmbedBuilder.createCompletedMatchEmbed(embedTitle, matchResult))
							.withContent(match.getAllMentions())
							.subscribe(msg -> msg.pin().subscribe());
					message.delete().subscribe();
				});
		bot.moveToArchive(game.getServer(), channel);
		bot.postToResultChannel(matchResult);
		dbService.saveMatchResult(matchResult);
		dbService.deleteMatch(match);
		matchResult.getPlayers().forEach(player -> {
			player.addMatchResult(matchResult);
			dbService.savePlayer(player);
			queueService.updatePlayerInAllQueuesOfGame(game, player);
			updatePlayerMatches(game, player);
		});
		boolean hasLeaderboardChanged = dbService.persistRankings(matchResult);
		if (hasLeaderboardChanged) bot.refreshLeaderboard(game).subscribe();
		for (Player player : match.getPlayers()) {
			bot.updatePlayerRank(game, player);
		}
	}

	public void processCancel(Match match, String reason) {
		bot.getMatchMessage(match)
				.subscribe(message -> {
					EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(reason, match);
					message.getChannel().subscribe(channel -> channel
							.createMessage(embedCreateSpec)
							.withContent(match.getAllMentions())
							.subscribe(msg -> msg.pin().subscribe()));
					message.delete().subscribe();
				});
		bot.getChannelById(match.getChannelId()).subscribe(channel -> bot.moveToArchive(match.getServer(), channel));
		dbService.deleteMatch(match);
	}

	public static ActionRow createActionRow(Match match) {
		UUID matchId = match.getId();
		if (match.getGame().isAllowDraw()) return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.draw(matchId),
				Buttons.cancel(matchId));
		else return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.cancel(matchId));
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
