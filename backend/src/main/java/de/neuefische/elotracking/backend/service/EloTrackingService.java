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
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
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
	private final TimedTaskQueue timedTaskQueue;
	@Getter
	private ApplicationPropertiesLoader propertiesLoader;

	public EloTrackingService(@Lazy DiscordBotService discordBotService, @Lazy GatewayDiscordClient client,
							  @Lazy TimedTaskQueue timedTaskQueue, ApplicationPropertiesLoader propertiesLoader,
							  GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao, PlayerDao playerDao) {
		this.bot = discordBotService;
		this.client = client;
		this.timedTaskQueue = timedTaskQueue;
		this.propertiesLoader = propertiesLoader;
		this.gameDao = gameDao;
		this.challengeDao = challengeDao;
		this.matchDao = matchDao;
		this.playerDao = playerDao;
		if (propertiesLoader.isDeleteDataOnStartup()
				&& !propertiesLoader.getSpringDataMongodbDatabase().equals("deploy")) {// make extra sure deploy does never get deleted
			deleteAllData();
		}
	}

	private void deleteAllData() {
		log.info("Deleting all data...");
		gameDao.deleteAll();
		challengeDao.deleteAll();
		matchDao.deleteAll();
		playerDao.deleteAll();
	}

	// Game
	public Optional<Game> findGameByGuildId(long guildId) {
		return gameDao.findById(guildId);
	}

	public void saveGame(Game game) {
		gameDao.save(game);
	}

	// Challenge
	public boolean challengeExistsByParticipants(long guildId, long challengerId, long acceptorId) {
		return challengeDao.findAllByChallengerId(challengerId).stream()
				.filter(challenge -> challenge.getAcceptorId() == acceptorId)
				.filter(challenge -> challenge.getGuildId() == guildId)
				.findAny().isPresent();
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

	public void timedDecayOpenChallenge(long challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = getChallengeByChallengerMessageId(challengeId);
		if (maybeChallenge.isEmpty()) return;
		ChallengeModel challenge = maybeChallenge.get();
		if (challenge.isAccepted()) return;

		deleteChallengeById(challengeId);
		Optional<Game> maybeGame = findGameByGuildId(challenge.getGuildId());
		if (maybeGame.isEmpty()) return;

		bot.sendToChannel(challenge.getGuildId(), String.format("<@%s> your open challenge towards <@%s> has expired after %s minutes",
				challenge.getChallengerId(), challenge.getAcceptorId(), time));
	}

	public void timedDecayAcceptedChallenge(long challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = getChallengeByChallengerMessageId(challengeId);
		if (maybeChallenge.isEmpty()) return;

		ChallengeModel challenge = maybeChallenge.get();
		deleteChallengeById(challengeId);
		Optional<Game> maybeGame = findGameByGuildId(challenge.getGuildId());
		if (maybeGame.isEmpty()) return;

		bot.sendToChannel(challenge.getGuildId(), String.format("<@%s> your match with <@%s> has expired after %s minutes",// TODO wochen, tage, etc
				challenge.getChallengerId(), challenge.getAcceptorId(), time));
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
	public void timedAutoResolveMatch(long challengeId, int time) {
		Optional<ChallengeModel> maybeChallenge = getChallengeByChallengerMessageId(challengeId);
		if (maybeChallenge.isEmpty()) return;

		ChallengeModel challenge = maybeChallenge.get();
		boolean reportIsByChallenger = challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED;
		ChallengeModel.ReportStatus report = reportIsByChallenger ? challenge.getChallengerReported() : challenge.getAcceptorReported();
		long channelId = challenge.getGuildId();
		long challengerId = challenge.getChallengerId();
		long acceptorId = challenge.getAcceptorId();
		long winnerId = report == ChallengeModel.ReportStatus.WIN ?
				reportIsByChallenger ? challengerId : acceptorId
				: reportIsByChallenger ? acceptorId : challengerId;
		long loserId = winnerId == challengerId ? acceptorId : challengerId;

		Match match = new Match(channelId, winnerId, loserId, false);
		double[] resolvedRatings = updateRatings(match);// TODO vllt umbauen
		deleteChallengeById(challenge.getChallengerMessageId());

		bot.sendToChannel(channelId, String.format("This match has been auto-resolved because only one player has reported the match after %d minutes:\n" +
						"<@%s> old rating %d, new rating %d. <@%s> old rating %d, new rating %d", time,
				winnerId, (int) resolvedRatings[0], (int) resolvedRatings[2],
				loserId, (int) resolvedRatings[1], (int) resolvedRatings[3]));
	}

	public void saveMatch(Match match) {
		matchDao.save(match);
	}

	// Player
	public boolean addNewPlayerIfPlayerNotPresent(long guildId, long playerId) {
		if (!playerDao.existsById(Player.generateId(guildId, playerId))) {
			playerDao.insert(new Player(guildId, playerId,
					initialRating));
			return true;
		}
		return false;
	}

	// Rankings
	public double[] updateRatings(Match match) {// TODO evtl match zurueckgeben
		Player winner = playerDao.findById(Player.generateId(match.getGuildId(), match.getWinnerId())).get();
		Player loser = playerDao.findById(Player.generateId(match.getGuildId(), match.getLoserId())).get();

		double[] ratings = calculateElo(winner.getRating(), loser.getRating(),
				match.isDraw() ? 0.5 : 1, k);

		match.setWinnerBeforeRating(winner.getRating());
		match.setWinnerAfterRating(ratings[2]);
		winner.setRating(ratings[2]);
		match.setLoserBeforeRating(loser.getRating());
		match.setLoserAfterRating(ratings[3]);
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

	public List<PlayerInRankingsDto> getRankings(long guildId) {
		List<Player> allPlayers = playerDao.findAllByGuildId(guildId);
		List<PlayerInRankingsDto> allPlayersAsDto = allPlayers.stream()
				.map(player -> new PlayerInRankingsDto(bot.getPlayerName(player.getUserId()), player.getRating()))
				.collect(Collectors.toList());
		Collections.sort(allPlayersAsDto);
		return allPlayersAsDto;
	}


	public void timedSummarizeMatch(long messageId, long channelId, Object value) {
		Match match = (Match) value;
		Message message = client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block();
		boolean isWinnerMessage = ((PrivateChannel) message.getChannel().block())
				.getRecipientIds().contains(Snowflake.of(match.getWinnerId()));
		System.out.println(isWinnerMessage);
		String opponentName = bot.getPlayerName(isWinnerMessage ? match.getLoserId() : match.getWinnerId());
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block()
				.edit().withContent(String.format("*You played a match against %s and %s. Your rating went from %s to %s.*",
						opponentName,
						match.isDraw() ? "drew :left_right_arrow:" : isWinnerMessage ? "won :up_arrow:" : "lost :down_arrow:",
						isWinnerMessage ? match.getWinnerBeforeRating() : match.getLoserBeforeRating(),
						isWinnerMessage ? match.getWinnerAfterRating() : match.getLoserAfterRating()))
				.subscribe();
	}

	public void timedDeleteMessage(long messageId, long channelId) {
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).subscribe();
	}
}
