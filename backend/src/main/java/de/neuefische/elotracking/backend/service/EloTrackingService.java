package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.configuration.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.dao.ChallengeDao;
import de.neuefische.elotracking.backend.dao.GameDao;
import de.neuefische.elotracking.backend.dao.MatchDao;
import de.neuefische.elotracking.backend.dao.PlayerDao;
import de.neuefische.elotracking.backend.dto.PlayerInRankingsDto;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.model.Player;
import discord4j.core.GatewayDiscordClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EloTrackingService {

	private static float initialRating = 1200;
	private static float k = 16;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final GameDao gameDao;
	private final ChallengeDao challengeDao;
	private final MatchDao matchDao;
	private final PlayerDao playerDao;
	@Getter
	private ApplicationPropertiesLoader propertiesLoader;

	public EloTrackingService(@Lazy DiscordBotService discordBotService, @Lazy GatewayDiscordClient client,
							  ApplicationPropertiesLoader propertiesLoader,
							  GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao, PlayerDao playerDao) {
		this.bot = discordBotService;
		this.client = client;
		this.propertiesLoader = propertiesLoader;
		this.gameDao = gameDao;
		this.challengeDao = challengeDao;
		this.matchDao = matchDao;
		this.playerDao = playerDao;

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
	}

	public void deleteAllDataForGameExceptGame(Game game) {
		matchDao.deleteAllByGuildId(game.getGuildId());
		challengeDao.deleteAllByGuildId(game.getGuildId());
		playerDao.deleteAllByGuildId(game.getGuildId());
	}

	// Game
	public void deleteGame(Game game) {
		gameDao.deleteById(game.getGuildId());
	}

	public Optional<Game> findGameByGuildId(long guildId) {
		return gameDao.findById(guildId);
	}

	public void saveGame(Game game) {
		gameDao.save(game);
	}

	public List<Game> findAllGames() {
		return gameDao.findAll();
	}

	// Challenge
	public Optional<ChallengeModel> findChallengeByParticipants(long guildId, long challengerId, long acceptorId) {
		return challengeDao.findAllByChallengerId(challengerId).stream()
				.filter(challenge -> challenge.getAcceptorId() == acceptorId)
				.filter(challenge -> challenge.getGuildId() == guildId)
				.findAny();
	}

	public boolean challengeExistsByAcceptorMessageId(long messageId) {
		return challengeDao.existsByAcceptorMessageId(messageId);
	}

	public boolean challengeExistsByChallengerMessageId(long messageId) {
		return challengeDao.existsByChallengerMessageId(messageId);
	}

	public Optional<ChallengeModel> getChallengeByChallengerMessageId(long messageId) {
		return challengeDao.findByChallengerMessageId(messageId);
	}

	public Optional<ChallengeModel> getChallengeById(long id) {
		return getChallengeByChallengerMessageId(id);
	}

	public Optional<ChallengeModel> getChallengeByAcceptorMessageId(long messageId) {
		return challengeDao.findByAcceptorMessageId(messageId);
	}

	public void saveChallenge(ChallengeModel challenge) {
		challengeDao.save(challenge);
	}

	public void deleteChallenge(ChallengeModel challenge) {
		deleteChallengeById(challenge.getChallengerMessageId());
	}

	public void deleteChallengeById(long id) {
		challengeDao.deleteById(id);
	}

	public List<ChallengeModel> findAllChallengesByAcceptorIdAndGuildId(long acceptorId, long channelId) {
		List<ChallengeModel> allChallenges = challengeDao.findAllByAcceptorId(acceptorId);
		List<ChallengeModel> filteredByChannel = allChallenges.stream().
				filter(challenge -> challenge.getGuildId() == channelId)
				.collect(Collectors.toList());
		return filteredByChannel;
	}

	public List<ChallengeModel> findAllChallengesByPlayerIdAndChannelId(long playerId, long channelId) {
		List<ChallengeModel> allChallengesForPlayer = new ArrayList<>();
		allChallengesForPlayer.addAll(challengeDao.findAllByChallengerId(playerId));
		allChallengesForPlayer.addAll(challengeDao.findAllByAcceptorId(playerId));

		List<ChallengeModel> filteredByChannel = allChallengesForPlayer.stream().
				filter(challenge -> challenge.getGuildId() == channelId)
				.collect(Collectors.toList());
		return filteredByChannel;
	}

	// Match
	public void saveMatch(Match match) {
		matchDao.save(match);
	}

	// Player
	public Optional<Player> findPlayerByGuildAndUserId(long guildId, long userId) {
		return playerDao.findById(Player.generateId(guildId, userId));
	}

	// Rankings
	public double[] updateRatings(Match match) {// TODO evtl match zurueckgeben
		addNewPlayerIfPlayerNotPresent(match.getGuildId(), match.getWinnerId());
		addNewPlayerIfPlayerNotPresent(match.getGuildId(), match.getLoserId());

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

		playerDao.save(winner);
		playerDao.save(loser);
		matchDao.save(match);

		return ratings;
	}

	private void addNewPlayerIfPlayerNotPresent(long guildId, long userId) {
		if (!playerDao.existsById(Player.generateId(guildId, userId))) {
			playerDao.insert(new Player(guildId, userId, initialRating));
		}
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
		return String.valueOf(Math.round(rating * 10) / 10);
	}

	public List<PlayerInRankingsDto> getRankings(long guildId) {
		List<Player> allPlayers = playerDao.findAllByGuildId(guildId);
		List<PlayerInRankingsDto> allPlayersAsDto = allPlayers.stream()
				.map(player -> new PlayerInRankingsDto(bot.getPlayerName(player.getUserId()), player.getRating()))
				.collect(Collectors.toList());
		Collections.sort(allPlayersAsDto);
		return allPlayersAsDto;
	}


}
