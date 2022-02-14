package com.elorankingbot.backend.service;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.dto.PlayerInRankingsDto;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Player;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@Slf4j
@Service
public class EloRankingService {

	private static float initialRating = 1200;
	private static float k = 16;
	private DiscordBotService bot;
	private GameDao gameDao;
	private ChallengeDao challengeDao;
	private MatchDao matchDao;
	private PlayerDao playerDao;
	private TimeSlotDao timeSlotDao;
	@Getter
	private ApplicationPropertiesLoader propertiesLoader;
	@Getter
	private Set<String> modCommands, adminCommands;

	@Autowired
	public EloRankingService(@Lazy DiscordBotService discordBotService, ApplicationPropertiesLoader propertiesLoader,
							 CommandClassScanner scanner,
							 GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao, PlayerDao playerDao,
							 TimeSlotDao timeSlotDao) {
		this.bot = discordBotService;
		this.propertiesLoader = propertiesLoader;
		this.gameDao = gameDao;
		this.challengeDao = challengeDao;
		this.matchDao = matchDao;
		this.playerDao = playerDao;
		this.timeSlotDao = timeSlotDao;
		this.adminCommands = scanner.getAdminCommands();
		this.modCommands = scanner.getModCommands();
	}

	public void deleteAllData() {
		if (propertiesLoader.getSpringDataMongodbDatabase().equals("deploy")) {
			throw new RuntimeException("deleteAllData is being called on deploy database");
		}
		log.info(String.format("Deleting all data on %s...", propertiesLoader.getSpringDataMongodbDatabase()));
		gameDao.deleteAll();
		challengeDao.deleteAll();
		matchDao.deleteAll();
		playerDao.deleteAll();
		timeSlotDao.deleteAll();
	}

	public void deleteAllDataForGame(long guildId) {
		matchDao.deleteAllByGuildId(guildId);
		challengeDao.deleteAllByGuildId(guildId);
		playerDao.deleteAllByGuildId(guildId);
		gameDao.deleteById(guildId);
	}

	public void resetAllPlayerRatings(long guildId) {
		List<Player> players = playerDao.findAllByGuildId(guildId);
		players.forEach(player -> player.setRating(1200));
		playerDao.saveAll(players);
	}

	// Game
	public Optional<Game> findGameByGuildId(long guildId) {// TODO kein optional zurueckgeben, fehler hier behandeln
		return gameDao.findById(guildId);
	}

	public void saveGame(Game game) {
		log.debug("saving game " + game.getName());
		gameDao.save(game);
	}

	public List<Game> findAllGames() {
		return gameDao.findAll();
	}

	public void deleteGame(long guildId) {
		gameDao.deleteById(guildId);
	}

	// Challenge
	public Optional<ChallengeModel> findChallengeByParticipants(long guildId, long challengerId, long acceptorId) {
		return challengeDao.findAllByGuildIdAndChallengerId(guildId, challengerId).stream()
				.filter(challenge -> challenge.getAcceptorId() == acceptorId)
				.findAny();
	}

	public Optional<ChallengeModel> findChallengeById(long id) {
		return challengeDao.findById(id);
	}

	public void saveChallenge(ChallengeModel challenge) {
		log.debug("saving challenge " + challenge.getId());
		challengeDao.save(challenge);
	}

	public void deleteChallenge(ChallengeModel challenge) {
		deleteChallengeById(challenge.getId());
	}

	public void deleteChallengeById(long id) {
		challengeDao.deleteById(id);
	}

	public List<ChallengeModel> findAllChallengesByGuildIdAndPlayerId(long guildId, long playerId) {
		List<ChallengeModel> allChallengesForPlayer = new ArrayList<>();
		allChallengesForPlayer.addAll(challengeDao.findAllByGuildIdAndChallengerId(guildId, playerId));
		allChallengesForPlayer.addAll(challengeDao.findAllByGuildIdAndAcceptorId(guildId, playerId));
		return allChallengesForPlayer;
	}

	// Match
	public void saveMatch(Match match) {
		log.debug(String.format("saving match %s %s %s",
				match.getWinnerId(), match.isDraw() ? "drew" : "defeated", match.getLoserId()));
		matchDao.save(match);
	}

	public void deleteMatch(Match match) {
		log.debug(String.format("deleting match %s %s %s",
				match.getWinnerId(), match.isDraw() ? "drew" : "defeated", match.getLoserId()));
		matchDao.delete(match);
	}

	public Optional<Match> findMostRecentMatch(long guildId, long player1Id, long player2Id) {
		Optional<Match> search = matchDao.findFirstByGuildIdAndWinnerIdAndLoserIdOrderByDate(guildId, player1Id, player2Id);
		Optional<Match> searchReverseParams = matchDao.findFirstByGuildIdAndWinnerIdAndLoserIdOrderByDate(guildId, player2Id, player1Id);

		if (search.isEmpty()) {
			return searchReverseParams;
		} else if (searchReverseParams.isEmpty()) {
			return search;
		} else {
			if (search.get().getDate().after(searchReverseParams.get().getDate())) {
				return search;
			} else {
				return searchReverseParams;
			}
		}
	}

	public List<Match> getMatchHistory(long playerId, long guildId) {
		List<Match> matches = matchDao.findAllByGuildIdAndWinnerId(guildId, playerId);
		matches.addAll(matchDao.findAllByGuildIdAndLoserId(guildId, playerId));
		Collections.sort(matches);
		return matches;
	}

	// Player
	public void savePlayer(Player player) {
		log.debug("saving player " + player.getUserId());
		playerDao.save(player);
	}

	public Optional<Player> findPlayerByGuildIdAndUserId(long guildId, long userId) {
		return playerDao.findById(Player.generateId(guildId, userId));
	}

	public void addNewPlayerIfPlayerNotPresent(long guildId, long userId) {// TODO! schauen wo man den namen schon hat
		if (!playerDao.existsById(Player.generateId(guildId, userId))) {
			playerDao.insert(new Player(guildId, userId, bot.getPlayerTag(userId), initialRating));
		}
	}

	public void addNewPlayerIfPlayerNotPresent(long guildId, long userId, String name) {
		if (!playerDao.existsById(Player.generateId(guildId, userId))) {
			playerDao.insert(new Player(guildId, userId, name, initialRating));
		}
	}

	// Rankings
	public double[] updateRatingsAndSaveMatchAndPlayers(Match match) {// TODO evtl match zurueckgeben
		Player winner = playerDao.findById(Player.generateId(match.getGuildId(), match.getWinnerId())).get();
		Player loser = playerDao.findById(Player.generateId(match.getGuildId(), match.getLoserId())).get();

		double[] ratings = calculateElo(winner.getRating(), loser.getRating(),
				match.isDraw() ? 0.5 : 1, k);

		match.setWinnerOldRating(winner.getRating());
		match.setWinnerNewRating(ratings[2]);
		winner.setRating(ratings[2]);
		match.setLoserOldRating(loser.getRating());
		match.setLoserNewRating(ratings[3]);
		loser.setRating(ratings[3]);
		if (match.isDraw()) {
			winner.addDraw();
			loser.addDraw();
		} else {
			winner.addWin();
			loser.addLoss();
		}

		savePlayer(winner);
		savePlayer(loser);
		saveMatch(match);

		return ratings;
	}

	private static double[] calculateElo(double rating1, double rating2, double player1Result, double k) {
		double player2Result = 1 - player1Result;
		double expectedResult1 = 1 / (1 + Math.pow(10, (rating2 - rating1) / 400));
		double expectedResult2 = 1 / (1 + Math.pow(10, (rating1 - rating2) / 400));
		double newRating1 = rating1 + k * (player1Result - expectedResult1);
		double newRating2 = rating2 + k * (player2Result - expectedResult2);
		return new double[]{rating1, rating2, newRating1, newRating2};
	}

	public static String formatRating(double rating) {
		return String.format("%.1f", Float.valueOf(Math.round(rating * 10)) / 10);
	}

	public List<PlayerInRankingsDto> getRankingsAsDto(long guildId) {
		List<Player> allPlayers = playerDao.findAllByGuildId(guildId);
		List<PlayerInRankingsDto> allPlayersAsDto = allPlayers.stream()
				.map(player -> new PlayerInRankingsDto(bot.getPlayerTag(player.getUserId()), player.getRating()))
				.collect(Collectors.toList());
		Collections.sort(allPlayersAsDto);
		return allPlayersAsDto;
	}

	public List<Player> getRankings(long guildId) {
		// TODO abfrage begrenzen und vorsortieren, der performance wegen
		// ueber rating indexieren, dann ne heuristik bauen die effizient die naechsten x umliegenden player findet
		List<Player> allPlayers = playerDao.findAllByGuildId(guildId);
		Collections.sort(allPlayers, Collections.reverseOrder());
		return allPlayers;
	}
}
