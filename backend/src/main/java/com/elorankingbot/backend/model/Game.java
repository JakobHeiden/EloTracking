package com.elorankingbot.backend.model;

import com.elorankingbot.backend.components.FormatTools;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    public String getVariable(String variableName) {
        switch (variableName) {
            case "Name" -> {
                return name;
            }
            case "Initial Rating" -> {
                return String.valueOf(initialRating);
            }
            case "K" -> {
                return String.valueOf(getQueues().stream().findAny().get().getK());
            }
            default -> {
                return "error";
            }
        }
    }

    public Optional<String> setVariable(String variableName, String value) {
        switch (variableName) {
            case "Name" -> {
                if (!FormatTools.isLegalDiscordName(value)) {
                    return Optional.of(FormatTools.illegalNameMessage());
                }
                server.getGameNameToGame().remove(name);
                name = value;
                server.getGameNameToGame().put(name, this);
                queueNameToQueue.keySet().forEach(queueName -> {
                    MatchFinderQueue queue = queueNameToQueue.get(queueName);
                    queue.setGame(this);
                    queueNameToQueue.put(queueName, queue);
                });
                return Optional.empty();
            }
            case "Initial Rating" -> {
                try {
                    initialRating = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // TODO different message for numbers that are too big
                    return Optional.of("Please enter an Integer.");
                }
                return Optional.empty();
            }
            case "K" -> {
                int newK;
                try {
                    newK = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return Optional.of("Please enter an Integer.");
                }
                if (newK <= 0) {
                    return Optional.of("Please enter a value larger than 0.");
                }
                getQueues().forEach(queue -> queue.setK(newK));
                return Optional.empty();
            }
            default -> {
                return Optional.of("error");
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
