package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.configuration.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.dao.*;
import de.neuefische.elotracking.backend.dto.PlayerInRankingsDto;
import de.neuefische.elotracking.backend.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EloTrackingService {
    private final DiscordBotService bot;
    private final GameDao gameDao;
    private final ChallengeDao challengeDao;
    private final MatchDao matchDao;
    private final PlayerDao playerDao;
    @Getter
    private final ApplicationPropertiesLoader config;

    @Autowired
    public EloTrackingService(@Lazy DiscordBotService discordBotService,
                              GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao, PlayerDao playerDao,
                              ApplicationPropertiesLoader applicationPropertiesLoader) {
        this.bot = discordBotService;
        this.gameDao = gameDao;
        this.challengeDao = challengeDao;
        this.matchDao = matchDao;
        this.playerDao = playerDao;
        this.config = applicationPropertiesLoader;
    }

    public Optional<Game> findGameByChannelId(String channelId) {
        return gameDao.findById(channelId);
    }

    public void saveGame(Game game) {
        gameDao.save(game);
    }

    public Game getGameData(String channelId) {
        return gameDao.findByChannelId(channelId);
    }

    public boolean challengeExistsById(String id) {
        return challengeDao.existsById(id);
    }

    public void addChallenge(String channelId, String challengerId, String otherPlayerId) {
        ChallengeModel newChallenge = challengeDao.insert(new ChallengeModel(channelId, challengerId, otherPlayerId));
    }

    public Optional<ChallengeModel> findChallenge(String challengeId) {
        return challengeDao.findById(challengeId);
    }

    public void saveChallenge(ChallengeModel challenge) {
        challengeDao.save(challenge);
    }

    public void deleteChallenge(ChallengeModel challengeModel) {
        challengeDao.delete(challengeModel);
    }

    public List<ChallengeModel> findAllChallengesOfRecipientForChannel(String recipientId, String channelId) {
        List<ChallengeModel> allChallenges = challengeDao.findAllByRecipientId(recipientId);
        List<ChallengeModel> filteredByChannel = allChallenges.stream().
                filter(challenge -> challenge.getChannelId().equals(channelId))
                .collect(Collectors.toList());
        return filteredByChannel;
    }

    public void saveMatch(Match match) {
        matchDao.save(match);
    }

    public boolean addNewPlayerIfPlayerNotPresent(String channelId, String playerId) {
        if (!playerDao.existsById(Player.generateId(channelId, playerId))) {
            playerDao.insert(new Player(channelId, playerId,
                    Float.parseFloat(config.getProperty("INITIAL_RATING"))));
            return true;
        }
        return false;
    }

    public double[] updateRatings(Match match) {
        Player winner = playerDao.findById(Player.generateId(match.getChannel(), match.getWinner())).get();
        Player loser = playerDao.findById(Player.generateId(match.getChannel(), match.getLoser())).get();
        double[] ratings = calculateElo(winner.getRating(), loser.getRating(),
                match.isDraw() ? 0.5 : 1,
                Float.parseFloat(config.getProperty("K")));
        winner.setRating(ratings[2]);
        loser.setRating(ratings[3]);
        playerDao.save(winner);
        playerDao.save(loser);
        match.setHasUpdatedPlayerRatings(true);
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
