package com.elorankingbot.backend.service;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.dto.PlayerInRankingsDto;
import com.elorankingbot.backend.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DBService {

	private static float initialRating = 1200;
	private static float k = 16;
	private final DiscordBotService bot;
	private final ServerDao serverDao;
	private final ChallengeDao challengeDao;
	private final MatchResultDao matchResultDao;
	private final PlayerDao playerDao;
	private final TimeSlotDao timeSlotDao;
	private final MatchDao matchDao;
	private final RankingsEntryDao rankingsEntryDao;
	@Getter
	private final Set<String> modCommands, adminCommands;// TODO weg

	@Autowired
	public DBService(Services services, CommandClassScanner scanner,
					 ServerDao serverDao, ChallengeDao challengeDao, MatchResultDao matchResultDao, PlayerDao playerDao,
					 TimeSlotDao timeSlotDao, MatchDao matchDao, RankingsEntryDao rankingsEntryDao) {
		this.bot = services.bot;
		this.serverDao = serverDao;
		this.challengeDao = challengeDao;
		this.matchResultDao = matchResultDao;
		this.playerDao = playerDao;
		this.timeSlotDao = timeSlotDao;
		this.adminCommands = scanner.getAdminCommandClassNames();
		this.modCommands = scanner.getModCommandClassNames();
		this.matchDao = matchDao;
		this.rankingsEntryDao = rankingsEntryDao;
	}


	public void deleteAllDataForGame(long guildId) {
		//matchDao.deleteAllByGuildId(guildId);
		//challengeDao.deleteAllByGuildId(guildId);
		//playerDao.deleteAllByGuildId(guildId);
		//gameDao.deleteById(guildId);
	}

	/*
	public void resetAllPlayerRatings(long guildId) {
		List<Player> players = playerDao.findAllByGuildId(guildId);
		//players.forEach(player -> player.setRating(1200));
		playerDao.saveAll(players);
	}

	 */

	// Server
	public Optional<Server> findServerByGuildId(long guildId) {
		return serverDao.findById(guildId);
	}

	public void saveServer(Server server) {
		serverDao.save(server);
	}

	public List<Server> findAllServers() {
		return serverDao.findAll();
	}

	// Match
	public Match getMatch(UUID matchId) {
		return matchDao.findById(matchId).get();
	}

	public void saveMatch(Match match) {
		log.debug("Saving match " + match.getId());
		matchDao.save(match);
	}

	public void deleteMatch(Match match) {
		log.debug("Deleting match " + match.getId());
		matchDao.delete(match);
	}

	// MatchResult
	public void saveMatchResult(MatchResult matchResult) {
		log.debug(String.format("saving match result %s", matchResult.getId()));
		matchResultDao.save(matchResult);
	}


	// Game
	// TODO das hier sollte obsolet sein, games sind teil von server
	/*
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

	 */

	// Challenge
	public Optional<ChallengeModel> findChallengeByParticipants(long guildId, long challengerId, long acceptorId) {
		//return challengeDao.findAllByGuildIdAndChallengerId(guildId, challengerId).stream()
		//		.filter(challenge -> challenge.getAcceptorUserId() == acceptorId)
		//		.findAny();
		return null;
	}

	public Optional<ChallengeModel> findChallengeById(UUID id) {
		return challengeDao.findById(id);
	}

	public void saveChallenge(ChallengeModel challenge) {
		log.debug("saving challenge " + challenge.getId());
		challengeDao.save(challenge);
	}

	public void deleteChallenge(ChallengeModel challenge) {
		//deleteChallengeById(challenge.getId());
	}

	public void deleteChallengeById(UUID id) {
		challengeDao.deleteById(id);
	}

	public List<ChallengeModel> findAllChallengesByGuildIdAndPlayerId(long guildId, long playerId) {
		List<ChallengeModel> allChallengesForPlayer = new ArrayList<>();
		//allChallengesForPlayer.addAll(challengeDao.findAllByGuildIdAndChallengerId(guildId, playerId));
		//allChallengesForPlayer.addAll(challengeDao.findAllByGuildIdAndAcceptorId(guildId, playerId));
		return allChallengesForPlayer;
	}

	// Match legacy


	public void deleteMatchResult(MatchResult matchResult) {
		//log.debug(String.format("deleting match %s %s %s",
		//		match.getWinnerId(), match.isDraw() ? "drew" : "defeated", match.getLoserId()));
		matchResultDao.delete(matchResult);
	}

	public Optional<MatchResult> findMostRecentMatchResult(long guildId, long player1Id, long player2Id) {
		return null;
		/*
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

		 */
	}

	public List<MatchResult> getMatchHistory(long playerId, long guildId) {
		return null;
		/*
		List<Match> matches = matchDao.findAllByGuildIdAndWinnerId(guildId, playerId);
		matches.addAll(matchDao.findAllByGuildIdAndLoserId(guildId, playerId));
		Collections.sort(matches);
		return matches;

		 */
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
		//	playerDao.insert(new Player(guildId, userId, bot.getPlayerTag(userId), initialRating));
		}
	}

	public void addNewPlayerIfPlayerNotPresent(long guildId, long userId, String name) {
		if (!playerDao.existsById(Player.generateId(guildId, userId))) {
		//	playerDao.insert(new Player(guildId, userId, name, initialRating));
		}
	}

	public Player getPlayerOrGenerateIfNotPresent(long guildId, long userId, String tag) {
		Optional<Player> maybePlayer = playerDao.findById(Player.generateId(guildId, userId));
		if (maybePlayer.isPresent()) return maybePlayer.get();

		Player player = new Player(guildId, userId, tag);
		playerDao.save(player);
		return player;
	}

	public Player getPlayer(long guildId, long userId) {
		return playerDao.findById(Player.generateId(guildId, userId)).get();
	}

	// Rankings
	public double[] updateRatingsAndSaveMatchAndPlayers(MatchResult matchResult) {// TODO evtl match zurueckgeben
		/*
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

		 */
		return null;
	}

	private static double[] calculateElo(double rating1, double rating2, double player1Result, double k) {
		double player2Result = 1 - player1Result;
		double expectedResult1 = 1 / (1 + Math.pow(10, (rating2 - rating1) / 400));
		double expectedResult2 = 1 / (1 + Math.pow(10, (rating1 - rating2) / 400));
		double newRating1 = rating1 + k * (player1Result - expectedResult1);
		double newRating2 = rating2 + k * (player2Result - expectedResult2);
		return new double[]{rating1, rating2, newRating1, newRating2};
	}

	public List<PlayerInRankingsDto> getRankingsAsDto(long guildId) {
		/*
		List<Player> allPlayers = playerDao.findAllByGuildId(guildId);
		List<PlayerInRankingsDto> allPlayersAsDto = allPlayers.stream()
				.map(player -> new PlayerInRankingsDto(bot.getPlayerTag(player.getUserId()), player.getRating()))
				.collect(Collectors.toList());
		Collections.sort(allPlayersAsDto);
		return allPlayersAsDto;

		 */
		return null;
	}

	public RankingsExcerpt getLeaderboard(Game game) {
		// TODO nur ausschnitte abrufen, wahrscheinlich mit PagingAndSortingRepository?
		List<RankingsEntry> allEntries = rankingsEntryDao.getAllByGuildIdAndAndGameName(game.getGuildId(), game.getName());
		int numTotalPlayers = allEntries.size();
		Collections.sort(allEntries);
		return new RankingsExcerpt(game, allEntries.subList(0, Math.min(game.getLeaderboardLength(), allEntries.size())),
				1, Optional.empty(), numTotalPlayers);
	}

	// TODO das hier nach MatchService?
	public boolean updateAndPersistRankingsAndPlayers(MatchResult matchResult) {
		matchResult.getAllPlayerMatchResults().forEach(playerMatchResult -> {
			PlayerGameStats gameStats = playerMatchResult.getPlayer().getGameStats(matchResult.getGame());
			gameStats.setRating(playerMatchResult.getNewRating());
			gameStats.addResultStatus(playerMatchResult.getResultStatus());
			savePlayer(playerMatchResult.getPlayer());
		});

		long guildId = matchResult.getGame().getServer().getGuildId();
		String gameName = matchResult.getGame().getName();
		for (PlayerMatchResult playerMatchResult : matchResult.getAllPlayerMatchResults()) {
			Optional<RankingsEntry> maybeRankingsEntry = rankingsEntryDao
					.findByGuildIdAndGameNameAndPlayerTag(guildId, gameName, playerMatchResult.getPlayerTag());
			maybeRankingsEntry.ifPresent(rankingsEntryDao::delete);
			rankingsEntryDao.save(new RankingsEntry(matchResult.getGame(), playerMatchResult.getPlayer()));
		}

		return true;// TODO! pagination etc
	}

	// am ende weg
	public Optional<Game> findGameByGuildId(long asLong) {
		return null;
	}


}
