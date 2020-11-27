package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.dao.ChallengeDao;
import de.neuefische.elotracking.backend.dao.GameDao;
import de.neuefische.elotracking.backend.dao.MatchDao;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Challenge;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.model.Match;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tinylog.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Service
public class EloTrackingService {
    private final DiscordBot bot;
    private final GameDao gameDao;
    private final ChallengeDao challengeDao;
    private final MatchDao matchDao;
    @Getter
    private Properties config = new Properties();

    @Autowired
    public EloTrackingService(@Lazy DiscordBot discordBot, GameDao gameDao, ChallengeDao challengeDao, MatchDao matchDao) throws IOException {
        this.bot = discordBot;
        this.gameDao = gameDao;
        this.challengeDao = challengeDao;
        this.matchDao = matchDao;
        this.config.load(new FileReader("backend/src/main/resources/config.txt"));
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

        return String.format(String.format("New game created. You can now %schallenge another player", bot.getPrefix()));
    }

    public String challenge(String channelId, String challengerId, String otherPlayerId) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register a new game", bot.getPrefix());
        }
        if (challengeDao.existsById(channelId + "-" + challengerId + "-" + otherPlayerId)) {
            return String.format("You already have an existing challenge towards that player. He needs to %saccept it" +
                    " before you can issue another", bot.getPrefix());
        }

        Challenge newChallenge = challengeDao.insert(new Challenge(channelId, challengerId, otherPlayerId));
        if (newChallenge == null) {
            Logger.error("Insert new Challenge to db failed: %s-%s-%s", channelId, challengerId, otherPlayerId);
            bot.sendToAdmin(String.format("Insert new Challenge to db failed: %s-%s-%s", channelId, challengerId, otherPlayerId));
            return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
        }

        return String.format("Challenge issued. Your opponent can now %saccept", bot.getPrefix());
    }

    public String accept(String channelId, String acceptingPlayerId, String challengerId) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register a new game", bot.getPrefix());
        }

        Optional<Challenge> challenge = challengeDao.findById(Challenge.generateId(channelId, challengerId, acceptingPlayerId));
        if (challenge.isEmpty()) {
            return "No unanswered challenge by that player";
        } else {
            challenge.get().accept();
            Challenge updatedChallenge = challengeDao.save(challenge.get());
            if (updatedChallenge == null) {
                Logger.error("Insert updated Challenge to db failed: %s-%s-%s", channelId, challengerId, acceptingPlayerId);
                bot.sendToAdmin(String.format("Insert updated Challenge to db failed: %s-%s-%s", channelId, challengerId, acceptingPlayerId));
                return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
            }

            return String.format("Challenge accepted! Come back and %sreport when your game is finished.", bot.getPrefix());
        }
    }

    public String report(String channelId, String reportingPlayerId, String reportedOnPlayerId, boolean isReportedWin) {
        if (!gameDao.existsByChannelId(channelId)) {
            return String.format("No game is associated with this channel. Use %sregister to register a new game", bot.getPrefix());
        }
        String challengeId = Challenge.generateId(channelId, reportingPlayerId, reportedOnPlayerId);
        if (!challengeDao.existsById(challengeId)) {
            return String.format("No challenge exists towards that player. Use %schallenge to issue one", bot.getPrefix());
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
                    Match newMatch = matchDao.insert(new Match(UUID.randomUUID(), channelId, new Date(),
                            reportedOnPlayerId, reportingPlayerId, false));
                    if (newMatch == null) {
                        Logger.error("Insert Match to db failed");
                        bot.sendToAdmin("Insert Match to db failed");
                        return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
                    }
                    challengeDao.delete(challenge);
                    return updateRatings(reportedOnPlayerId, reportingPlayerId, false);
                }
            case LOSS:
                if (isReportedWin) {
                    Match newMatch = matchDao.insert(new Match(UUID.randomUUID(), channelId, new Date(),
                        reportingPlayerId, reportedOnPlayerId, false));
                    if (newMatch == null) {
                        Logger.error("Insert Match to db failed");
                        bot.sendToAdmin("Insert Match to db failed");
                        return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
                    }
                    challengeDao.delete(challenge);
                    return updateRatings(reportingPlayerId, reportedOnPlayerId, false);
                } else {
                    return "Both players reported a loss. Please contact your game admin";
                }
            default:
                return String.format("%s reported. The other player needs to report as well so the match " +
                        "can be processed", isReportedWin ? "Win" : "Loss");
        }
    }

    private String updateRatings(String reportedOnPlayerId, String reportingPlayerId, boolean b) {
        //TODO implement
        return "TODO";
    }

    private static double[] calculateElo(double rating1, double rating2, float player1Result, float k) {
        float player2Result = 1 - player1Result;
        double expectedResult1 = 1 / (1 + Math.pow(10, (rating2-rating1)/400));
        double expectedResult2 = 1 / (1 + Math.pow(10, (rating1-rating2)/400));
        double newRating1 = rating1 + k * (player1Result - expectedResult1);
        double newRating2 = rating2 + k * (player2Result - expectedResult2);
        return new double[] {newRating1, newRating2};
    }
}
