package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.command.MessageContent;
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
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.http.client.ClientException;
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
	private final TimedTaskQueue queue;
	@Getter
	private ApplicationPropertiesLoader propertiesLoader;

	public EloTrackingService(@Lazy DiscordBotService discordBotService, @Lazy GatewayDiscordClient client,
							  @Lazy TimedTaskQueue queue, ApplicationPropertiesLoader propertiesLoader,
							  GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao, PlayerDao playerDao) {
		this.bot = discordBotService;
		this.client = client;
		this.queue = queue;
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
		Optional<ChallengeModel> maybeChallenge = getChallengeById(challengeId);
		if (maybeChallenge.isEmpty()) return;
		ChallengeModel challenge = maybeChallenge.get();
		if (challenge.isDispute()) return;

		boolean hasChallengerReported = challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED;
		ChallengeModel.ReportStatus report = hasChallengerReported ?
				challenge.getChallengerReported()
				: challenge.getAcceptorReported();
		long challengerId = challenge.getChallengerId();
		long acceptorId = challenge.getAcceptorId();
		long winnerId = 0L;
		long loserId = 0L;
		boolean isDraw = false;
		boolean isWin = false;
		switch (report) {
			case WIN:
				winnerId = hasChallengerReported ? challengerId : acceptorId;
				loserId = hasChallengerReported ? acceptorId : challengerId;
				isWin = true;
				break;
			case LOSE:
				winnerId = hasChallengerReported ? acceptorId : challengerId;
				loserId = hasChallengerReported ? challengerId : acceptorId;
				break;
			case DRAW:
				winnerId = challengerId;
				loserId = acceptorId;
				isDraw = true;
				break;
			case CANCEL:
				timedAutoResolveMatchAsCancel(challenge, hasChallengerReported);
		}

		Game game = findGameByGuildId(challenge.getGuildId()).get();
		Match match = new Match(challenge.getGuildId(), winnerId, loserId, isDraw);
		updateRatings(match);
		deleteChallengeById(challenge.getId());
		postToInvolvedChannelsAndAddTimedTask(challenge, match, game, hasChallengerReported, isDraw, isWin);
		bot.postToResultChannel(game, match);
	}

	private void postToInvolvedChannelsAndAddTimedTask(ChallengeModel challenge, Match match, Game game,
													   boolean hasChallengerReported, boolean isDraw, boolean isWin) {
		Message reportPresentMessage = hasChallengerReported ?
				bot.getChallengerMessage(challenge).block()
				: bot.getAcceptorMessage(challenge).block();
		int reportPresentOldRating = (int) Math.round(isDraw ?
				hasChallengerReported ? match.getWinnerOldRating() : match.getLoserOldRating()
				: isWin ? match.getWinnerOldRating() : match.getLoserOldRating());
		int reportPresentNewRating = (int) Math.round(isDraw ?
				hasChallengerReported ? match.getWinnerNewRating() : match.getLoserNewRating()
				: isWin ? match.getWinnerNewRating() : match.getLoserNewRating());
		MessageContent reportPresentMessageContent = new MessageContent(reportPresentMessage.getContent())
				.makeAllNotBold()
				.addLine(String.format("Your opponent has failed to report within %s minutes. " +
								"The match is getting resolved according to your report now.",
						game.getMatchAutoResolveTime()))
				.addLine(String.format("Your rating went from %s to %s.",
						reportPresentOldRating, reportPresentNewRating))
				.makeAllItalic();
		reportPresentMessage.edit().withContent(reportPresentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		Message reportAbsentMessage = hasChallengerReported ?
				bot.getAcceptorMessage(challenge).block()
				: bot.getChallengerMessage(challenge).block();
		int reportAbsentOldRating = (int) Math.round(isDraw ?
				hasChallengerReported ? match.getLoserOldRating() : match.getWinnerOldRating()
				: isWin ? match.getLoserOldRating() : match.getWinnerOldRating());
		int reportAbsentNewRating = (int) Math.round(isDraw ?
				hasChallengerReported ? match.getLoserNewRating() : match.getWinnerNewRating()
				: isWin ? match.getLoserNewRating() : match.getWinnerNewRating());
		MessageContent reportAbsentMessageContent = new MessageContent(reportAbsentMessage.getContent())
				.makeAllNotBold()
				.addLine(String.format("You have failed to report within %s minutes. " +
								"The match is getting resolved according to your opponent's report now.",
						game.getMatchAutoResolveTime()))
				.addLine(String.format("Your rating went from %s to %s.",
						reportAbsentOldRating, reportAbsentNewRating))
				.makeAllItalic();
		reportAbsentMessage.edit().withContent(reportAbsentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), match);
	}

	private void timedAutoResolveMatchAsCancel(ChallengeModel challenge, boolean hasChallengerReported) {
		Game game = findGameByGuildId(challenge.getGuildId()).get();
		deleteChallenge(challenge);

		Message reportPresentMessage = hasChallengerReported ?
				bot.getChallengerMessage(challenge).block()
				: bot.getAcceptorMessage(challenge).block();
		MessageContent reportPresentMessageContent = new MessageContent(reportPresentMessage.getContent())
				.makeAllNotBold()
				.addLine("Your opponent has failed to report within %s minutes. " +
						"The match is canceled.")
				.makeAllItalic();
		reportPresentMessage.edit().withContent(reportPresentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		Message reportAbsentMessage = hasChallengerReported ?
				bot.getAcceptorMessage(challenge).block()
				: bot.getChallengerMessage(challenge).block();
		MessageContent reportAbsentMessageContent = new MessageContent(reportAbsentMessage.getContent())
				.makeAllNotBold()
				.addLine("You have failed to report within %s minutes. " +
						"The match is canceled.")
				.makeAllItalic();
		reportAbsentMessage.edit().withContent(reportAbsentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				reportPresentMessage.getId().asLong(), reportPresentMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				reportAbsentMessage.getId().asLong(), reportAbsentMessage.getChannelId().asLong(), null);
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

	public Optional<Player> findPlayerByGuildAndUserId(long guildId, long userId) {
		return playerDao.findById(Player.generateId(guildId, userId));
	}

	// Rankings
	public double[] updateRatings(Match match) {// TODO evtl match zurueckgeben
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
						match.isDraw() ? "drew :left_right_arrow:" : isWinnerMessage ? "won :arrow_up:" : "lost :arrow_down:",
						isWinnerMessage ? Math.round(match.getWinnerOldRating()) : Math.round(match.getLoserOldRating()),
						isWinnerMessage ? Math.round(match.getWinnerNewRating()) : Math.round(match.getLoserNewRating())))
				.subscribe();
	}

	public void timedDeleteMessage(long messageId, long channelId) {
		client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)).block().delete().subscribe();
	}

	public void timedDeleteChannel(long channelId) {
		try {
			client.getChannelById(Snowflake.of(channelId)).block()
					.delete().subscribe();
		} catch (ClientException ignored) {
		}
	}
}
