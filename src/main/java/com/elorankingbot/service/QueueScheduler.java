package com.elorankingbot.service;

import com.elorankingbot.logging.ExceptionHandler;
import com.elorankingbot.model.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@CommonsLog
public class QueueScheduler {

    private final DBService dbService;
    private final DiscordBotService bot;
    private final MatchService matchService;
    private final ExceptionHandler exceptionHandler;

    public QueueScheduler(Services services) {
        this.dbService = services.dbService;
        this.bot = services.bot;
        this.matchService = services.matchService;
        this.exceptionHandler = services.exceptionHandler;
    }

    @Scheduled(fixedRate = 3000)
    public void generateAndStartMatches() {
        dbService.findAllServers().stream()
                .flatMap(server -> server.getGames().stream())
                .flatMap(game -> game.getQueues().stream()).forEach(
                        queue -> {
                            try {
                                boolean foundMatch;
                                do {
                                    Optional<Match> maybeMatch = generateMatchIfPossible(queue);
                                    if (maybeMatch.isPresent()) {
                                        for (Player player : maybeMatch.get().getPlayers())
                                            removePlayerFromAllQueues(queue.getServer(), player);
                                        matchService.startMatch(maybeMatch.get());
                                        foundMatch = true;
                                    } else {
                                        foundMatch = false;
                                    }
                                } while (foundMatch);
                            } catch (Exception e) {
                                String context = String.format("%s::generateAndStartMatches on %s:%s:%s",
                                        this.getClass().getSimpleName(), bot.getServerIdAndName(queue.getServer()),
                                        queue.getGame().getName(), queue.getName());
                                exceptionHandler.handleException(e, context);
                            }
                        });
    }

    public Optional<Match> generateMatchIfPossible(MatchFinderQueue queue) {
        if (queue.getQueueType() == MatchFinderQueue.QueueType.SOLO) return generateMatchFromSoloQueue(queue);
        if (queue.getQueueType() == MatchFinderQueue.QueueType.PREMADE) return generateMatchFromPremadeQueue(queue);
        return null;
    }

    public Optional<Match> generateMatchFromSoloQueue(MatchFinderQueue queue) {
        List<Group> groupsSortedByRating = new LinkedList<>(queue.getGroups().stream().sorted().toList());
        Date now = new Date();

        int numPlayersNeeded = queue.getNumPlayersPerMatch();
        if (groupsSortedByRating.size() < numPlayersNeeded) return Optional.empty();
        for (int i = 0; i <= groupsSortedByRating.size() - numPlayersNeeded; i++) {// TODO abwechseln von unten und von oben matches suchen
            List<Group> potentialMatch = groupsSortedByRating.subList(i, i + numPlayersNeeded);
            if (queue.getMaxRatingSpread() == MatchFinderQueue.NO_LIMIT)
                return Optional.of(buildMatch(potentialMatch, queue));
            double potentialHighestRating = potentialMatch.stream()
                    .mapToDouble(group -> group.getAverageRating() - group.getRatingElasticity(now, queue))
                    .max().getAsDouble();
            double potentialLowestRating = potentialMatch.stream()
                    .mapToDouble(group -> group.getAverageRating() + group.getRatingElasticity(now, queue))
                    .min().getAsDouble();
            log.trace(String.format("%.1f - %.1f = %.1f <? %s", potentialHighestRating, potentialLowestRating,
                    potentialHighestRating - potentialLowestRating, queue.getMaxRatingSpread()));
            if (potentialHighestRating - potentialLowestRating <= queue.getMaxRatingSpread())
                return Optional.of(buildMatch(potentialMatch, queue));
        }
        return Optional.empty();
    }

    private Match buildMatch(List<Group> groups, MatchFinderQueue queue) {
        List<List<Player>> teams = new ArrayList<>();
        for (int i = 0; i < queue.getNumTeams(); i++) {
            teams.add(new ArrayList<>());
        }
        for (int i = 0; i < queue.getNumPlayersPerTeam(); i += 2) {
            for (int j = 0; j < queue.getNumTeams(); j++) {
                if (queue.getNumPlayersPerTeam() - i > 1) {
                    // take a player from top and bottom
                    teams.get(j).add(groups.get(0).getPlayers().get(0));
                    groups.remove(0);
                    teams.get(j).add(groups.get(groups.size() - 1).getPlayers().get(0));
                    groups.remove(groups.size() - 1);
                } else {// TODO besser waere der player in der mitte statt random
                    // take a random player
                    int randomIndex = ThreadLocalRandom.current().nextInt(0, groups.size());
                    teams.get(j).add(groups.get(randomIndex).getPlayers().get(0));
                    groups.remove(randomIndex);
                }
            }
        }
        return new Match(queue, teams);
    }

    private Optional<Match> generateMatchFromPremadeQueue(MatchFinderQueue queue) {
        if (queue.getGroups().size() < queue.getNumTeams()) return Optional.empty();

        List<List<Player>> allPlayers = queue.getGroups().stream()
                .map(Group::getPlayers)
                .collect(Collectors.toList());
        return Optional.of(new Match(queue, allPlayers));
    }

    public void removePlayerFromAllQueues(Server server, Player player) {
        server.getGames().stream()
                .flatMap(game -> game.getQueueNameToQueue().values().stream())
                .forEach(queue -> queue.removeGroupsContainingPlayer(player));
        dbService.saveServer(server);

        player.setLastJoinedQueueAt(null);
        dbService.savePlayer(player);
    }

    public void updatePlayerInAllQueuesOfGame(Game game, Player player) {
        boolean hasGameChanged = false;
        for (MatchFinderQueue queue : game.getQueues()) {
            boolean hasQueueChanged = queue.updatePlayerIfPresent(player);
            if (hasQueueChanged) hasGameChanged = true;
        }
        if (hasGameChanged) dbService.saveServer(game.getServer());
    }
}
