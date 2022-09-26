package com.elorankingbot.backend.service;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.model.*;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DBService {

	private final DiscordBotService bot;
	private final ServerDao serverDao;
	private final ChallengeDao challengeDao;
	private final MatchResultDao matchResultDao;
	private final MatchResultReferenceDao matchResultReferenceDao;
	private final PlayerDao playerDao;
	private final MatchDao matchDao;
	private final RankingsEntryDao rankingsEntryDao;
	private final BotStatsAccumulatorDao botStatsAccumulatorDao;
	private final BotStatsDao botStatsDao;
	private final ApplicationPropertiesLoader props;

	@Autowired
	public DBService(Services services,
					 ServerDao serverDao, ChallengeDao challengeDao, MatchResultDao matchResultDao,
					 MatchResultReferenceDao matchResultReferenceDao, PlayerDao playerDao,
					 MatchDao matchDao, RankingsEntryDao rankingsEntryDao, BotStatsAccumulatorDao botStatsAccumulatorDao,
					 BotStatsDao botStatsDao) {
		this.bot = services.bot;
		this.serverDao = serverDao;
		this.challengeDao = challengeDao;
		this.matchResultDao = matchResultDao;
		this.matchResultReferenceDao = matchResultReferenceDao;
		this.playerDao = playerDao;
		this.matchDao = matchDao;
		this.rankingsEntryDao = rankingsEntryDao;
		this.botStatsAccumulatorDao = botStatsAccumulatorDao;
		this.botStatsDao = botStatsDao;
		this.props = services.props;
	}

	public void resetAllPlayerRatings(Game game) {
		log.debug(String.format("Resetting all player ratings for %s on %s", game.getName(), bot.getServerName(game.getServer())));
		List<Player> players = playerDao.findAllByGuildId(game.getGuildId());
		players.forEach(player -> {
			PlayerGameStats gameStats = player.getOrCreateGameStats(game);
			gameStats.setRating(game.getInitialRating());
			gameStats.setWins(0);
			gameStats.setLosses(0);
			gameStats.setDraws(0);
		});
		playerDao.saveAll(players);
		rankingsEntryDao.deleteAllByGuildIdAndAndGameName(game.getGuildId(), game.getName());
	}

	// Server
	public Server getOrCreateServer(long guildId) {
		Optional<Server> maybeServer = serverDao.findById(guildId);
		if (maybeServer.isPresent()) {
			Server server = maybeServer.get();
			if (server.isMarkedForDeletion()) {
				server.setMarkedForDeletion(false);
				serverDao.save(server);
			}
			return server;
		} else {
			Server newServer = new Server(guildId);
			serverDao.save(newServer);
			bot.sendToOwner("New server: " + guildId);
			return newServer;
		}
	}

	public void saveServer(Server server) {
		log.debug(String.format("Saving server %s", bot.getServerName(server)));
		serverDao.save(server);
	}

	public List<Server> findAllServers() {
		return serverDao.findAll();
	}

	public void deleteServerAndAssociatedData(Server server) {
		playerDao.deleteAllByGuildId(server.getGuildId());
		matchDao.deleteAllByServer(server);
		matchResultDao.deleteAllByServer(server);
		rankingsEntryDao.deleteAllByGuildId(server.getGuildId());
		serverDao.deleteById(server.getGuildId());
	}

	// Match
	public Match getMatch(UUID matchId) {
		return matchDao.findById(matchId).get();
	}

	public Optional<Match> findMatch(UUID matchId) {
		return matchDao.findById(matchId);
	}

	public void saveMatch(Match match) {
		log.debug(String.format("Saving match %s on %s: %s",
				match.getId(),
				bot.getServerName(match.getServer()),
				match.getPlayers().stream().map(Player::getTag).collect(Collectors.joining(","))));
		matchDao.save(match);
	}

	public void deleteMatch(Match match) {
		log.debug(String.format("Deleting match %s on %s: %s",
				match.getId(),
				bot.getServerName(match.getServer()),
				match.getPlayers().stream().map(Player::getTag).collect(Collectors.joining(","))));
		matchDao.delete(match);
	}

	public void deleteAllMatches(Game game) {
		log.debug(String.format("Deleting all match for %s on %s", game.getName(), bot.getServerName(game.getServer())));
		matchDao.deleteAllByServerAndGameId(game.getServer(), game.getName());
	}

	public List<Match> findAllMatchesByServer(Server server) {
		return matchDao.findAllByServer(server);
	}

	public List<Match> findAllMatchesByPlayer(Player player) {
		return findAllMatchesByServer(getOrCreateServer(player.getGuildId()))
				.stream().filter(match -> match.containsPlayer(player.getId()))
				.toList();
	}

	// MatchResult
	public void saveMatchResult(MatchResult matchResult) {
		log.debug(String.format("saving match result %s for %s on %s",
				matchResult.getId(),
				matchResult.getGame().getName(),
				bot.getServerName(matchResult.getServer())));
		matchResultDao.save(matchResult);
	}

	public Optional<MatchResult> findMatchResult(UUID id) {
		return matchResultDao.findById(id);
	}

	public void deleteAllMatchResults(Game game) {
		log.debug(String.format("Deleting all match results for %s on %s",
				game.getName(),
				bot.getServerName(game.getServer())));
		matchResultDao.deleteAllByServerAndGameName(game.getServer(), game.getName());
	}

	// MatchResultReference
	public void saveMatchResultReference(MatchResultReference matchResultReference) {
		matchResultReferenceDao.save(matchResultReference);
	}

	public Optional<MatchResultReference> findMatchResultReference(long messageId) {
		Optional<MatchResultReference> maybeMatchResultReference = matchResultReferenceDao.findByResultMessageId(messageId);
		if (maybeMatchResultReference.isEmpty()) {
			maybeMatchResultReference = matchResultReferenceDao.findByMatchMessageId(messageId);
			if (maybeMatchResultReference.isEmpty()) {
				return Optional.empty();
			}
		}
		return maybeMatchResultReference;
	}

	// Player
	public void savePlayer(Player player) {
		log.debug(String.format("saving player %s on %s", player.getTag(), bot.getServerName(player)));
		playerDao.save(player);
	}

	public void saveAllPlayers(List<Player> players) {
		log.debug(String.format("Saving players %s on %s",
				String.join(",", players.stream().map(Player::getTag).toList()),
				players.isEmpty() ? "unknown" : bot.getServerName(players.get(0))));
		playerDao.saveAll(players);
	}

	public Optional<Player> findPlayerByGuildIdAndUserId(long guildId, long userId) {
		return playerDao.findById(Player.generateId(guildId, userId));
	}

	public Player getPlayerOrGenerateIfNotPresent(long guildId, User user) {
		Optional<Player> maybePlayer = playerDao.findById(Player.generateId(guildId, user.getId().asLong()));
		if (maybePlayer.isPresent()) return maybePlayer.get();

		Player player = new Player(guildId, user.getId().asLong(), user.getTag());
		playerDao.save(player);
		return player;
	}

	public List<Player> findAllPlayersForServer(Server server) {
		return playerDao.findAllByGuildId(server.getGuildId());
	}

	// Rankings
	public RankingsExcerpt getLeaderboard(Game game) {
		// TODO nur ausschnitte abrufen, wahrscheinlich mit PagingAndSortingRepository?
		List<RankingsEntry> allEntries = rankingsEntryDao.getAllByGuildIdAndAndGameName(game.getGuildId(), game.getName());
		int numTotalPlayers = allEntries.size();
		Collections.sort(allEntries);
		return new RankingsExcerpt(game, allEntries.subList(0, Math.min(game.getLeaderboardLength(), allEntries.size())),
				1, Optional.empty(), numTotalPlayers);
	}

	public RankingsExcerpt getRankingsExcerptForPlayer(Game game, Player player) {
		// TODO wie oben
		List<RankingsEntry> allEntries = rankingsEntryDao.getAllByGuildIdAndAndGameName(game.getGuildId(), game.getName());
		int numTotalPlayers = allEntries.size();
		Collections.sort(allEntries);
		Optional<Integer> maybePlayerRankingsEntryIndex = allEntries.stream()
				.filter(rankingsEntry -> rankingsEntry.getPlayerTag().equals(player.getTag()))
				.map(allEntries::indexOf).findAny();

		List<RankingsEntry> entries;
		int lowestIndex;
		if (maybePlayerRankingsEntryIndex.isPresent()) {
			int centerIndex = maybePlayerRankingsEntryIndex.get();
			int excerptLength = 20;// TODO
			lowestIndex = Math.max(0, centerIndex - excerptLength / 2);// TODO immer 20 zeigen, auch am rand
			int highestIndex = Math.min(allEntries.size(), centerIndex + excerptLength / 2);
			entries = allEntries.subList(lowestIndex, highestIndex);
		} else {
			entries = new ArrayList<>();
			lowestIndex = -2;
		}

		return new RankingsExcerpt(game, entries, lowestIndex + 1, Optional.of(player.getTag()), numTotalPlayers);
	}

	public boolean updateRankingsEntries(MatchResult matchResult) {
		for (PlayerMatchResult playerMatchResult : matchResult.getAllPlayerMatchResults()) {
			Optional<RankingsEntry> maybeRankingsEntry = rankingsEntryDao
					.findByGuildIdAndGameNameAndPlayerTag(matchResult.getGame().getServer().getGuildId(),
							matchResult.getGame().getName(), playerMatchResult.getPlayerTag());
			maybeRankingsEntry.ifPresent(rankingsEntryDao::delete);
			RankingsEntry newRankingsEntry = new RankingsEntry(matchResult.getGame(), playerMatchResult.getPlayer());
			rankingsEntryDao.save(newRankingsEntry);
		}
		return hasLeaderboardChanged(matchResult);
	}

	private boolean hasLeaderboardChanged(MatchResult matchResult) {
		int leaderboardLength = matchResult.getGame().getLeaderboardLength();
		List<RankingsEntry> leaderboard = rankingsEntryDao.findTopByGuildIdAndGameName(
				matchResult.getServer().getGuildId(),
				matchResult.getGame().getName(),
				PageRequest.of(0, leaderboardLength));
		if (leaderboard.size() < leaderboardLength)
			return true;
		double lowestLeaderboardRating = leaderboard.get(leaderboardLength - 1).getRating();
		for (PlayerMatchResult playerMatchResult : matchResult.getAllPlayerMatchResults()) {
			Optional<RankingsEntry> maybeRankingsEntry = rankingsEntryDao
					.findByGuildIdAndGameNameAndPlayerTag(matchResult.getGame().getServer().getGuildId(),
							matchResult.getGame().getName(), playerMatchResult.getPlayerTag());
			if (maybeRankingsEntry.isPresent() && leaderboard.contains(maybeRankingsEntry.get())) {
				return true;
			}
			if (playerMatchResult.getNewRating() >= lowestLeaderboardRating) {
				return true;
			}
		}
		return false;
	}

	public boolean hasLeaderboardChanged(Game game, double oldRating, double newRating) {
		int leaderboardLength = game.getLeaderboardLength();
		List<RankingsEntry> leaderboard = rankingsEntryDao.findTopByGuildIdAndGameName(
				game.getServer().getGuildId(),
				game.getName(),
				PageRequest.of(0, leaderboardLength));
		if (leaderboard.size() < leaderboardLength)	{
			return true;
		}
		double lowestLeaderboardRating = leaderboard.get(leaderboardLength - 1).getRating();
		return lowestLeaderboardRating < Math.max(oldRating, newRating);
	}


	public void deleteAllRankingsEntries(Game game) {
		rankingsEntryDao.deleteAllByGuildIdAndAndGameName(game.getGuildId(), game.getName());
	}

	// Statistics
	public void addMatchResultToStats(MatchResult matchResult) {
		if (props.getTestServerIds().contains(matchResult.getServer().getGuildId())) {
			return;
		}

		var maybeAccumulator = botStatsAccumulatorDao.findById(BotStatsAccumulator.SINGLETON_ID);
		BotStatsAccumulator accumulator = maybeAccumulator.isEmpty() ? new BotStatsAccumulator() : maybeAccumulator.get();
		accumulator.addMatchResult(matchResult);
		botStatsAccumulatorDao.save(accumulator);
	}

	public void persistBotStatsAndRestartAccumulator() {
		Optional<BotStatsAccumulator> maybeAccumulator = botStatsAccumulatorDao.findById(BotStatsAccumulator.SINGLETON_ID);
		if (maybeAccumulator.isPresent()) {
			botStatsDao.save(BotStats.botStatsOf(maybeAccumulator.get()));
			botStatsAccumulatorDao.save(new BotStatsAccumulator());
		} else {
			String warnMessage = "BotStatsAccumulator not found.";
			log.warn(warnMessage);
			bot.sendToOwner(warnMessage);
		}
	}

}
