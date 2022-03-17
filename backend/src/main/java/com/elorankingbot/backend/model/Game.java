package com.elorankingbot.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
public class Game {

    @Id
    private String name;
    private Map<String, MatchFinderQueue> queues;
    @DBRef(lazy = true)
    private Server server;
    private int leaderboardLength;
    private boolean allowDraw;
    private int matchAutoResolveTime;
    private int messageCleanupTime;
    private int noReportsModalityDecayTime;

    public Game(Server server, String name, boolean allowDraw) {
        this.name = name;
        this.server = server;
        this.queues = new HashMap<>();
        this.leaderboardLength = 20;
        this.allowDraw = allowDraw;
        this.matchAutoResolveTime = 24 * 60;
        this.messageCleanupTime = 12 * 60;
        this.noReportsModalityDecayTime = 7 * 24 * 60;
    }

    public void addQueue(MatchFinderQueue queue) {
        queues.put(queue.getName(), queue);
    }

    public MatchFinderQueue getQueue(String name) {
        return queues.get(name);
    }

    public boolean hasSingularQueue() {
        return queues.size() == 1;
    }

    public long getGuildId() {
        return server.getGuildId();
    }

    @Override
    public String toString() {
        return name;
    }
}
