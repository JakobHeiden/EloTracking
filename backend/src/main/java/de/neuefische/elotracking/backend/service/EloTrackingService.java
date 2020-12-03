package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.common.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.dao.*;
import de.neuefische.elotracking.backend.dto.PlayerInRankingsDto;
import de.neuefische.elotracking.backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tinylog.Logger;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EloTrackingService {
    private final DiscordBot bot;
    private final GameDao gameDao;
    private final ChallengeDao challengeDao;
    private final MatchDao matchDao;
    private final PlayerDao playerDao;
    private final ApplicationPropertiesLoader config;

    @Autowired
    public EloTrackingService(@Lazy DiscordBot discordBot, GameDao gameDao,
                              ChallengeDao challengeDao, MatchDao matchDao,
                              PlayerDao playerDao,
                              ApplicationPropertiesLoader applicationPropertiesLoader) {
        this.bot = discordBot;
        this.gameDao = gameDao;
        this.challengeDao = challengeDao;
        this.matchDao = matchDao;
        this.playerDao = playerDao;
        this.config = applicationPropertiesLoader;
    }

    public String register(String channelId, String name) {
        if (gameDao.existsByChannelId(channelId)) {
            String nameOfExistingGame = gameDao.findByChannelId(channelId).getName();
            return String.format("There is already a game associated with this channel: %s", nameOfExistingGame);
        }

        Game newGame = gameDao.insert(new Game(channelId, name));
        if (newGame == null) {
            Logger.error("Insert name Game to db failed: %s %s", channelId, name);
            bot.sendToAdmin(String.format("Insert new Game to db failed: %s %s", channelId, name));
            return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
        }

        return String.format(String.format("New game created. You can now %schallenge another player", config.getProperty("DEFAULT_PREFIX")));
    }

    public String challenge(String channelId, String challengerId, String otherPlayerId) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register a new game", config.getProperty("DEFAULT_PREFIX"));
        }
        if (challengeDao.existsById(channelId + "-" + challengerId + "-" + otherPlayerId)) {
            return String.format("You already have an existing challenge towards that player. He needs to %saccept it" +
                    " before you can issue another", config.getProperty("INITIAL_RATING"));
        }

        addNewPlayerIfPlayerNotPresent(channelId, challengerId);

        Challenge newChallenge = challengeDao.insert(new Challenge(channelId, challengerId, otherPlayerId));
        if (newChallenge == null) {
            Logger.error("Insert new Challenge to db failed: %s-%s-%s", channelId, challengerId, otherPlayerId);
            bot.sendToAdmin(String.format("Insert new Challenge to db failed: %s-%s-%s", channelId, challengerId, otherPlayerId));
            return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
        }

        return String.format("Challenge issued. Your opponent can now %saccept", config.getProperty("DEFAULT_PREFIX"));
    }

    public String accept(String channelId, String acceptingPlayerId, String challengerId) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register a new game", config.getProperty("DEFAULT_PREFIX"));
        }

        Optional<Challenge> challenge = challengeDao.findById(Challenge.generateId(channelId, challengerId, acceptingPlayerId));
        if (challenge.isEmpty()) {
            return "No unanswered challenge by that player";
        } else {
            addNewPlayerIfPlayerNotPresent(channelId, acceptingPlayerId);

            challenge.get().accept();
            Challenge updatedChallenge = challengeDao.save(challenge.get());
            if (updatedChallenge == null) {
                Logger.error("Insert updated Challenge to db failed: %s-%s-%s", channelId, challengerId, acceptingPlayerId);
                bot.sendToAdmin(String.format("Insert updated Challenge to db failed: %s-%s-%s", channelId, challengerId, acceptingPlayerId));
                return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
            }

            return String.format("Challenge accepted! Come back and %sreport when your game is finished.", config.getProperty("DEFAULT_PREFIX"));
        }
    }

    private boolean addNewPlayerIfPlayerNotPresent(String channelId, String playerId) {
        if (!playerDao.existsById(Player.generateId(channelId, playerId))) {
            playerDao.insert(new Player(channelId, playerId,
                    Float.parseFloat(config.getProperty("INITIAL_RATING"))));
            return true;
        }
        return false;
    }

    public String report(String channelId, String reportingPlayerId, String reportedOnPlayerId, boolean isReportedWin) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register a new game", config.getProperty("DEFAULT_PREFIX"));
        }
        String challengeId = Challenge.generateId(channelId, reportingPlayerId, reportedOnPlayerId);
        if (!challengeDao.existsById(challengeId)) {
            return String.format("No challenge exists towards that player. Use %schallenge to issue one", config.getProperty("DEFAULT_PREFIX"));
        }
        Challenge challenge = challengeDao.findById(challengeId).get();
        if (challenge.getAcceptedWhen().isEmpty()) {
            return "This challenge has not been accepted yet and cannot be reported as a win";
        }

        //do the actual reporting
        Challenge.ReportStatus reportedOnPlayerReportStatus =
                challenge.report(reportingPlayerId.equals(challenge.getChallengerId()), isReportedWin);
        challengeDao.save(challenge);

        //check if the challenge can be resolved into a match
        switch (reportedOnPlayerReportStatus) {
            case WIN:
                if (isReportedWin) {
                    return "Both players reported a win. Please contact your game admin";
                } else {
                    Match match = matchDao.insert(new Match(UUID.randomUUID(), channelId, new Date(),
                            reportedOnPlayerId, reportingPlayerId, false, false));
                    double[] ratings = updateRatings(match);
                    challengeDao.delete(challenge);
                    return String.format("%s old rating %d, new rating %d. %s old rating %d, new rating %d",
                            "%s", (int) ratings[0], (int) ratings[2], "%s", (int) ratings[1], (int) ratings[3]);
                }
            case LOSS:
                if (isReportedWin) {
                    Match match = matchDao.insert(new Match(UUID.randomUUID(), channelId, new Date(),
                        reportingPlayerId, reportedOnPlayerId, false, false));
                    double[] ratings = updateRatings(match);
                    challengeDao.delete(challenge);
                    return String.format("%s old rating %d, new rating %d. %s old rating %d, new rating %d",
                            "%s", (int) ratings[0], (int) ratings[2], "%s", (int) ratings[1], (int) ratings[3]);

                } else {
                    return "Both players reported a loss. Please contact your game admin";
                }
            default:
                return String.format("%s reported. The other player needs to report as well so the match " +
                        "can be processed", isReportedWin ? "Win" : "Loss");
        }
    }

    private double[] updateRatings(Match match) {
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
        double expectedResult1 = 1 / (1 + Math.pow(10, (rating2-rating1)/400));
        double expectedResult2 = 1 / (1 + Math.pow(10, (rating1-rating2)/400));
        double newRating1 = rating1 + k * (player1Result - expectedResult1);
        double newRating2 = rating2 + k * (player2Result - expectedResult2);
        return new double[] {rating1, rating2, newRating1, newRating2};
    }

    public List<PlayerInRankingsDto> getRankings(String channelId) {
        List<Player> allPlayers = playerDao.findAllByChannelId(channelId);
        List<PlayerInRankingsDto> allPlayersAsDto = allPlayers.stream()
                .map(player -> new PlayerInRankingsDto(bot.getPlayerName(player.getDiscordUserId()), player.getRating()))
                .collect(Collectors.toList());
        Collections.sort(allPlayersAsDto);
        return allPlayersAsDto;
    }

    public Game getGameData(String channelId) {
        return gameDao.findByChannelId(channelId);
    }

    public String setprefix(String channelId, String newPrefix) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register" +
                    " a new game", config.getProperty("DEFAULT_PREFIX"));
        }

        Game game = gameDao.findByChannelId(channelId);
        game.setCommandPrefix(newPrefix);
        gameDao.save(game);
        return String.format("Command prefix changed to %s", newPrefix);
    }

    public boolean isCommand(String channelId, String firstCharacter) {
        if (!gameDao.existsByChannelId(channelId)) {
            return (firstCharacter.equals(config.getProperty("DEFAULT_PREFIX")));
        } else {
            return (firstCharacter.equals(gameDao.findByChannelId(channelId).getCommandPrefix()));
        }
    }
}
