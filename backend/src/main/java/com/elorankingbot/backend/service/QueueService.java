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

	private class GroupRatingComparator implements Comparator<Group> {

		public int compare(Group a, Group b) {
			return (int) Math.ceil(getAverageRating(a, a.getGame()) - getAverageRating(b, b.getGame()));
		}
	}

	@Scheduled(fixedRate = 5000)
	public void generateAndStartMatches() {
		dbService.findAllServers().stream()
				.flatMap(server -> server.getGames().stream())
				.flatMap(game -> game.getQueues().stream()).forEach(queue -> {
					boolean foundMatch;
					do {
						Optional<Match> maybeMatch = generateMatchIfPossible(queue);
						if (maybeMatch.isPresent()) {
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

	private Optional<Match> generateMatchFromSoloQueue(MatchFinderQueue queue) {
		if (queue.getGroups().size() < queue.getNumTeams() * queue.getNumPlayersPerTeam()) return Optional.empty();

		List<Group> groupsSortedByRating = new LinkedList<>(
				queue.getGroups().stream()
						.sorted(new GroupRatingComparator())
						.toList());

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

	private double getAverageRating(Group group, Game game) {
		double sumOfRatings = group.getPlayers().stream()
				.map(player -> player.getGameStats(game).getRating())
				.reduce(0D, Double::sum);
		return sumOfRatings / group.getPlayers().size();
	}

	public boolean isPlayerInQueue(Player player, MatchFinderQueue queue) {// TODO das gehoert hier nicht her, eher an server, oder queue?
		return queue.getGroups().stream().anyMatch(group -> group.hasPlayer(player));
	}

	public void removePlayerFromAllQueues(Server server, Player player) {
		server.getGames().stream()
				.flatMap(game -> game.getQueueNameToQueue().values().stream())
				.forEach(queue -> queue.removeGroupsContainingPlayer(player));
		dbService.saveServer(server);
	}
}
