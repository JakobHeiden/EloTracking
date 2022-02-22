package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class QueueService {

	private class RatingComparator implements Comparator<Group> {

		public int compare(Group a, Group b) {
			return (int) Math.ceil(getAverageRating(a, a.getGame()) - getAverageRating(b, b.getGame()));
		}
	}

	public Optional<Match> generateMatchIfPossible(MatchFinderQueue queue) {
		if (queue.getQueueType() == MatchFinderQueue.QueueType.SOLO) return generateMatchFromSoloQueue(queue);
		if (queue.getQueueType() == MatchFinderQueue.QueueType.PREMADE) return generateMatchFromPremadeQueue(queue);
		return null;
	}

	private Optional<Match> generateMatchFromSoloQueue(MatchFinderQueue queue) {
		if (queue.getGroups().size() < queue.getNumTeams() * queue.getPlayersPerTeam()) return Optional.empty();

		List<Player> playersSortedByRating = queue.getGroups().stream()
				.sorted(new RatingComparator())
				.map(group -> group.getPlayers().get(0))
				.toList();

		List<List<Player>> teams = new ArrayList<>();
		for (int i = 0; i < queue.getNumTeams(); i++) {
			teams.add(new ArrayList<>());
		}
		for (int i = 0; i < queue.getPlayersPerTeam(); i+=2) {
			for (int j = 0; j < queue.getNumTeams(); j++) {
				if (queue.getPlayersPerTeam() - i > 1) {
					teams.get(j).add(playersSortedByRating.get(0));
					playersSortedByRating.remove(0);
					teams.get(j).add(playersSortedByRating.get(playersSortedByRating.size() - 1));
					playersSortedByRating.remove(playersSortedByRating.size() - 1);
				} else {
					int randomIndex = ThreadLocalRandom.current().nextInt(0, playersSortedByRating.size() + 1);
					teams.get(j).add(playersSortedByRating.get(randomIndex));
					playersSortedByRating.remove(randomIndex);
				}
			}
		}

		return Optional.of(new Match(queue.getGame(), teams));
	}

	private Optional<Match> generateMatchFromPremadeQueue(MatchFinderQueue queue) {
		if (queue.getGroups().size() < queue.getNumTeams()) return Optional.empty();

		List<List<Player>> allPlayers = queue.getGroups().stream()
				.map(Group::getPlayers)
				.collect(Collectors.toList());
		return Optional.of(new Match(queue.getGame(), allPlayers));
	}

	private double getAverageRating(Group group, Game game) {
		double sumOfRatings = group.getPlayers().stream()
				.map(player -> player.getRatings().get(game.getName()).getValue())
				.reduce(0D, Double::sum);
		return sumOfRatings / group.getPlayers().size();
	}
}
