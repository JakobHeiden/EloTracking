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
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
	private final GameDao gameDao;
	private final ChallengeDao challengeDao;
	private final MatchDao matchDao;
	private final PlayerDao playerDao;
	private final TimedTaskQueue timedTaskQueue;
	@Getter
	private ApplicationPropertiesLoader propertiesLoader;

	@Autowired
	public EloTrackingService(@Lazy DiscordBotService discordBotService, @Lazy TimedTaskQueue timedTaskQueue, ApplicationPropertiesLoader propertiesLoader,
							  GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao, PlayerDao playerDao) {
		this.bot = discordBotService;
		this.timedTaskQueue = timedTaskQueue;
		this.propertiesLoader = propertiesLoader;
		this.gameDao = gameDao;
		this.challengeDao = challengeDao;
		this.matchDao = matchDao;
		this.playerDao = playerDao;
	}

	// Game
	public Optional<Game> findGameByChannelId(String channelId) {
		return gameDao.findById(channelId);
	}

	public void saveGame(Game game) {
		gameDao.save(game);
	}

	// Challenge
	public boolean challengeExistsById(String id) {
		return challengeDao.existsById(id);
	}

	public Optional<ChallengeModel> findChallenge(String challengeId) {
		return challengeDao.findById(challengeId);
	}

	public void saveChallenge(ChallengeModel challenge) {
		challengeDao.save(challenge);
	}

	public void deleteChallenge(String id) {
		challengeDao.deleteById(id);
	}

	public void timedDecayOpenChallenge(String challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = findChallenge(challengeId);
		if (maybeChallenge.isEmpty()) return;
		ChallengeModel challenge = maybeChallenge.get();
		if (challenge.isAccepted()) return;

		deleteChallenge(challengeId);
		Optional<Game> maybeGame = findGameByChannelId(challenge.getChannelId());
		if (maybeGame.isEmpty()) return;

		bot.sendToChannel(challenge.getChannelId(), String.format("<@%s> your open challenge towards <@%s> has expired after %s minutes",
				challenge.getChallengerId(), challenge.getAcceptorId(), time));
	}

	public void timedDecayAcceptedChallenge(String challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = findChallenge(challengeId);
		if (maybeChallenge.isEmpty()) return;

		ChallengeModel challenge = maybeChallenge.get();
		deleteChallenge(challengeId);
		Optional<Game> maybeGame = findGameByChannelId(challenge.getChannelId());
		if (maybeGame.isEmpty()) return;

		bot.sendToChannel(challenge.getChannelId(), String.format("<@%s> your match with <@%s> has expired after %s minutes",// TODO wochen, tage, etc
				challenge.getChallengerId(), challenge.getAcceptorId(), time));
	}

	public List<ChallengeModel> findAllChallengesByAcceptorIdAndChannelId(String acceptorId, String channelId) {
		List<ChallengeModel> allChallenges = challengeDao.findAllByAcceptorId(acceptorId);
		List<ChallengeModel> filteredByChannel = allChallenges.stream().
				filter(challenge -> challenge.getChannelId().equals(channelId))
				.collect(Collectors.toList());
		return filteredByChannel;
	}

	public List<ChallengeModel> findAllChallengesByPlayerIdAndChannelId(String playerId, String channelId) {
		List<ChallengeModel> allChallengesForPlayer = new ArrayList<>();
		allChallengesForPlayer.addAll(challengeDao.findAllByChallengerId(playerId));
		allChallengesForPlayer.addAll(challengeDao.findAllByAcceptorId(playerId));

		List<ChallengeModel> filteredByChannel = allChallengesForPlayer.stream().
				filter(challenge -> challenge.getChannelId().equals(channelId))
				.collect(Collectors.toList());
		return filteredByChannel;
	}

	// Match
	public void timedAutoResolveMatch(String challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = findChallenge(challengeId);
		if (maybeChallenge.isEmpty()) return;

		ChallengeModel challenge = maybeChallenge.get();
		boolean reportIsByChallenger = challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED;
		ChallengeModel.ReportStatus report = reportIsByChallenger ? challenge.getChallengerReported() : challenge.getAcceptorReported();
		String channelId = challenge.getChannelId();
		String challengerId = challenge.getChallengerId();
		String acceptorId = challenge.getAcceptorId();
		String winnerId = report == ChallengeModel.ReportStatus.WIN ?
				reportIsByChallenger ? challengerId : acceptorId
				: reportIsByChallenger ? acceptorId : challengerId;
		String loserId = winnerId.equals(challengerId) ? acceptorId : challengerId;

		Match match = new Match(channelId, winnerId, loserId, false);
		double[] resolvedRatings = updateRatings(match);// TODO vllt umbauen
		deleteChallenge(challenge.getId());

		bot.sendToChannel(channelId, String.format("This match has been auto-resolved because only one player has reported the match after %d minutes:\n" +
						"<@%s> old rating %d, new rating %d. <@%s> old rating %d, new rating %d", time,
				winnerId, (int) resolvedRatings[0], (int) resolvedRatings[2],
				loserId, (int) resolvedRatings[1], (int) resolvedRatings[3]));
	}

	public void saveMatch(Match match) {
		matchDao.save(match);
	}

	// Player
	public boolean addNewPlayerIfPlayerNotPresent(String channelId, String playerId) {
		if (!playerDao.existsById(Player.generateId(channelId, playerId))) {
			playerDao.insert(new Player(channelId, playerId,
					initialRating));
			return true;
		}
		return false;
	}

	// Rankings
	public double[] updateRatings(Match match) {
		Player winner = playerDao.findById(Player.generateId(match.getChannel(), match.getWinner())).get();
		Player loser = playerDao.findById(Player.generateId(match.getChannel(), match.getLoser())).get();
		double[] ratings = calculateElo(winner.getRating(), loser.getRating(),
				match.isDraw() ? 0.5 : 1, k);
		winner.setRating(ratings[2]);
		loser.setRating(ratings[3]);
		playerDao.save(winner);
		playerDao.save(loser);
		matchDao.save(match);
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

	public List<PlayerInRankingsDto> getRankings(String channelId) {
		List<Player> allPlayers = playerDao.findAllByChannelId(channelId);
		List<PlayerInRankingsDto> allPlayersAsDto = allPlayers.stream()
				.map(player -> new PlayerInRankingsDto(bot.getPlayerName(player.getDiscordUserId()), player.getRating()))
				.collect(Collectors.toList());
		Collections.sort(allPlayersAsDto);
		return allPlayersAsDto;
	}
}
