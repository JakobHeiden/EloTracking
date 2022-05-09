package com.elorankingbot.backend.timedtask;

import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.ReportStatus;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.TextChannel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TimedTaskService {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final TimedTaskQueue queue;
	private final GatewayDiscordClient client;

	protected final List none = new ArrayList<>();

	public TimedTaskService(Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.queue = services.timedTaskQueue;
		this.client = services.client;
	}

	public void markGamesForDeletion() {
		List<Long> allGuildIds = client.getGuilds()
				.map(guild -> guild.getId().asLong())
				.collectList().block();
		//service.findAllGames().stream()
		//		.filter(game -> !allGuildIds.contains(game.getGuildId()))
		//		.forEach(game -> game.setMarkedForDeletion(true));
	}

	public void deleteGamesMarkedForDeletion() {
		//service.findAllGames().stream()
		//		.filter(game -> game.isMarkedForDeletion())
		//		.forEach(game -> service.deleteGame(game.getGuildId()));
	}

	public void summarizeMatch(long messageId, long channelId, Object value) {
		/*
		Match match = (Match) value;
		Message message = client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block();
		boolean isWinnerMessage = ((PrivateChannel) message.getChannel().block())
				.getRecipientIds().contains(Snowflake.of(match.getWinnerId()));
		String opponentName = bot.getPlayerTag(isWinnerMessage ? match.getLoserId() : match.getWinnerId());
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block()
				.edit().withContent(String.format("*You played a match against %s and %s. Your rating went from %s to %s.*",
						opponentName,
						match.isDraw() ? "drew :left_right_arrow:" : isWinnerMessage ? "won :arrow_up:" : "lost :arrow_down:",
						isWinnerMessage ? service.formatRating(match.getWinnerOldRating()) : service.formatRating(match.getLoserOldRating()),
						isWinnerMessage ? service.formatRating(match.getWinnerNewRating()) : service.formatRating(match.getLoserNewRating())))
				.subscribe();

		 */
	}

	public void deleteMessage(long messageId, long channelId) {
		// client uses messageId and channelId in reverse order
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block().delete().subscribe();
	}

	public void deleteChannel(long channelId) {
		client.getChannelById(Snowflake.of(channelId)).block()
				.delete().onErrorContinue((ignored, o) -> {}).subscribe();
	}

	public void unbanPlayer(long guildId, long userId, int duration) {
		Player player = dbService.findPlayerByGuildIdAndUserId(guildId, userId).get();
		if (!player.isBanned()) return;
		if (player.getUnbanAtTimeSlot() != queue.getCurrentIndex()) return;

		player.setUnbanAtTimeSlot(-2);
		dbService.savePlayer(player);

		bot.getPrivateChannelByUserId(userId).subscribe(privateChannel -> privateChannel
				.createMessage(String.format("Your ban has run out after %s. You are no longer banned.",
						DurationParser.minutesToString(duration))).subscribe());
	}

	public void warnMissingReports(UUID matchId) {
		Optional<Match> maybeMatch = dbService.findMatch(matchId);
		if (maybeMatch.isEmpty()) return;

		Match match = maybeMatch.get();
		StringBuilder mentions = new StringBuilder();
		for (Player player : match.getPlayers()) {
			if (match.getReportStatus(player.getId()).equals(ReportStatus.NOT_YET_REPORTED)) {
				mentions.append(String.format("<@%s>", player.getUserId()));
			}
		}
		TextChannel channel = (TextChannel) bot.getChannelById(match.getChannelId()).block();
		channel.createMessage(mentions + " there are 10 minutes left to make your report. After that, I will autoresolve " +
				"the match if possible, or otherwise open a dispute.").subscribe();// TODO
	}
}
