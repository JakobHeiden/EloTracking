package com.elorankingbot.timedtask;

import com.elorankingbot.model.Match;
import com.elorankingbot.model.Player;
import com.elorankingbot.model.ReportStatus;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.DBService;
import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.QueueScheduler;
import com.elorankingbot.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@CommonsLog
public class TimedTaskService {

    private final DBService dbService;
    private final DiscordBotService bot;
    private final TimedTaskScheduler queue;
    private final GatewayDiscordClient client;
    private final QueueScheduler queueScheduler;

    protected final List none = new ArrayList<>();

    public TimedTaskService(Services services) {
        this.dbService = services.dbService;
        this.bot = services.bot;
        this.queue = services.timedTaskScheduler;
        this.client = services.client;
        this.queueScheduler = services.queueScheduler;
    }

    void markServersForDeletion(List<Long> allGuildIds) {
        dbService.findAllServers().stream()
                .filter(server -> !allGuildIds.contains(server.getGuildId()))
                .forEach(server -> {
                    server.setMarkedForDeletion(true);
                    dbService.saveServer(server);
                    log.info("marking for deletion: " + server.getGuildId());
                });
    }

    void unmarkServersForDeletionIfAgainPresent(List<Long> allGuildIds) {
        dbService.findAllServers().stream()
                .filter(Server::isMarkedForDeletion)
                .filter(server -> allGuildIds.contains(server.getGuildId()))
                .forEach(server -> {
                    server.setMarkedForDeletion(false);
                    dbService.saveServer(server);
                    log.info(String.format("unmarking for deletion %s", server.getGuildId()));
                });
    }

    void deleteServersMarkedForDeletion() {
        dbService.findAllServers().stream()
                .filter(Server::isMarkedForDeletion)
                .forEach(server -> {
                    String missingAccessMessage = String.format("Missing Access to guild %s. Deleting Server.", server.getGuildId());
                    bot.sendToOwner(missingAccessMessage);
                    log.warn(missingAccessMessage);
                    dbService.deleteServerAndAssociatedData(server);
                });
    }

    void summarizeMatch(long messageId, long channelId, Object value) {
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

    void deleteMessage(long messageId, long channelId) {
        // client uses messageId and channelId in reverse order
        client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block().delete().subscribe();
    }

    void deleteChannel(long channelId) {
        try {
            bot.getChannelById(channelId).block().delete().subscribe();
        } catch (Exception ignored) {
        }
    }

    void unbanPlayer(long guildId, long userId, int duration) {
        Player player = dbService.findPlayerByGuildIdAndUserId(guildId, userId).get();
        if (!player.isBanned()) return;
        if (player.getUnbanAtTimeSlot() != queue.getCurrentIndex()) return;

        player.setUnbanAtTimeSlot(-2);
        dbService.savePlayer(player);

        bot.sendDM(userId, String.format("Your ban has run out after %s. You are no longer banned.",
                DurationParser.minutesToString(duration)));
    }

    void warnMissingReports(UUID matchId) {
        Optional<Match> maybeMatch = dbService.findMatch(matchId);
        if (maybeMatch.isEmpty()) return;

        Match match = maybeMatch.get();
        StringBuilder mentions = new StringBuilder();
        for (Player player : match.getPlayers()) {
            if (match.getReportStatus(player.getId()).equals(ReportStatus.NOT_YET_REPORTED)) {
                mentions.append(String.format("<@%s>", player.getUserId()));
            }
        }
        if (mentions.isEmpty()) return;

        ((TextChannel) bot.getChannelById(match.getChannelId()).block())// TODO exception wenn der channel weg ist fangen
                .createMessage(mentions + " there are 10 minutes left to make your report. After that, I will autoresolve " +
                        "the match if possible, or otherwise open a dispute.").subscribe();// TODO
    }

    void leaveQueues(long userId, long guildId, int duration, Object when) {
        Player player = dbService.findPlayerByGuildIdAndUserId(guildId, userId).get();
        if (player.getLastJoinedQueueAt() == null) return;
        if (!when.equals(player.getLastJoinedQueueAt())) return;

        queueScheduler.removePlayerFromAllQueues(dbService.getOrCreateServer(guildId), player);
        bot.sendDM(userId, String.format("I removed you from all queues you were in because it has been %s since you last joined a queue.",
                DurationParser.minutesToString(duration)));
    }
}
