package com.elorankingbot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Game {

    @Id
    @EqualsAndHashCode.Include
    private String name;
    private Map<String, MatchFinderQueue> queueNameToQueue;
    @DBRef(lazy = true)
    private Server server;
    private long resultChannelId;
    private long leaderboardMessageId;
    private long leaderboardChannelId;
    private int leaderboardLength;
    private boolean allowDraw;
    private int matchAutoResolveTime;
    private int messageCleanupTime;
    private int noReportsModalityDecayTime;
    private Map<Integer, Long> requiredRatingToRankId;
    private int initialRating;

    public Game(Server server, String name, boolean allowDraw) {
        this.name = name;
        this.queueNameToQueue = new HashMap<>();
        this.server = server;
        this.resultChannelId = 0L;
        this.leaderboardLength = 20;
        this.allowDraw = allowDraw;
        this.matchAutoResolveTime = 24 * 60;
        this.messageCleanupTime = 12 * 60;
        this.noReportsModalityDecayTime = 7 * 24 * 60;
        this.requiredRatingToRankId = new HashMap<>();
        this.initialRating = 1200;
    }

    public void addQueue(MatchFinderQueue queue) {
        queueNameToQueue.put(queue.getName().toLowerCase(), queue);
    }

    public void deleteQueue(String name) {
        queueNameToQueue.remove(name);
    }

    public MatchFinderQueue getQueue(String name) {
        return queueNameToQueue.get(name.toLowerCase());
    }

    public Collection<MatchFinderQueue> getQueues() {
        return queueNameToQueue.values();
    }

    public boolean hasSingularQueue() {
        return queueNameToQueue.size() == 1;
    }

    public long getGuildId() {
        return server.getGuildId();
    }

    @Override
    public String toString() {
        return name;
    }
}
