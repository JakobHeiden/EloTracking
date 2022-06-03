package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class QueueService {

	private final DBService dbService;
	private final MatchService matchService;

	public QueueService(Services services) {
		this.dbService = services.dbService;
		this.matchService = services.matchService;
	}

	// TODO logging
	@Scheduled(fixedRate = 3000)
	public void generateAndStartMatches() {
		dbService.findAllServers().stream()
				.flatMap(server -> server.getGames().stream())
				.flatMap(game -> game.getQueues().stream()).forEach(queue -> {
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
			System.out.println(potentialHighestRating);
			double potentialLowestRating = potentialMatch.stream()
					.mapToDouble(group -> group.getAverageRating() + group.getRatingElasticity(now, queue))
					.min().getAsDouble();
			System.out.println(potentialLowestRating);
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

	// TODO weg
	private Optional<Match> alt_generateMatchFromSoloQueue(MatchFinderQueue queue) {
		if (queue.getGroups().size() < queue.getNumTeams() * queue.getNumPlayersPerTeam()) return Optional.empty();

		List<Group> groupsSortedByRating = new LinkedList<>(
				queue.getGroups().stream()
						.sorted().toList());

		List<List<Player>> teams = new ArrayList<>();
		for (int i = 0; i < queue.getNumTeams(); i++) {
			teams.add(new ArrayList<>());
		}
		for (int i = 0; i < queue.getNumPlayersPerTeam(); i += 2) {
			for (int j = 0; j < queue.getNumTeams(); j++) {
				if (queue.getNumPlayersPerTeam() - i > 1) {
					// take a player from top and bottom
					teams.get(j).add(groupsSortedByRating.get(0).getPlayers().get(0));
					groupsSortedByRating.remove(0);
					teams.get(j).add(groupsSortedByRating.get(groupsSortedByRating.size() - 1).getPlayers().get(0));
					groupsSortedByRating.remove(groupsSortedByRating.size() - 1);
				} else {
					// take a random player
					int randomIndex = ThreadLocalRandom.current().nextInt(0, groupsSortedByRating.size());
					teams.get(j).add(groupsSortedByRating.get(randomIndex).getPlayers().get(0));
					groupsSortedByRating.remove(randomIndex);
				}
			}
		}

		return Optional.of(new Match(queue, teams));
	}

	private Optional<Match> generateMatchFromPremadeQueue(MatchFinderQueue queue) {
		if (queue.getGroups().size() < queue.getNumTeams()) return Optional.empty();

		List<List<Player>> allPlayers = queue.getGroups().stream()
				.map(Group::getPlayers)
				.collect(Collectors.toList());
		return Optional.of(new Match(queue, allPlayers));
	}

	public boolean isPlayerInQueue(Player player, MatchFinderQueue queue) {// TODO das gehoert hier nicht her, eher an server, oder queue?
		return queue.getGroups().stream().anyMatch(group -> group.hasPlayer(player));
	}

	public void removePlayerFromAllQueues(Server server, Player player) {
		server.getGames().stream()
				.flatMap(game -> game.getQueueNameToQueue().values().stream())
				.forEach(queue -> queue.removeGroupsContainingPlayer(player));
		dbService.saveServer(server);// TODO nur wenn noetig...
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
