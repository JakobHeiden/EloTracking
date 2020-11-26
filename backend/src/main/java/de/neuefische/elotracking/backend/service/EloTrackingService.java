package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.dao.ChallengeDao;
import de.neuefische.elotracking.backend.dao.GameDao;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Challenge;
import de.neuefische.elotracking.backend.model.Game;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tinylog.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

@Service
public class EloTrackingService {
    private final DiscordBot bot;
    private final GameDao gameDao;
    private final ChallengeDao challengeDao;
    @Getter
    private Properties config = new Properties();

    @Autowired
    public EloTrackingService(@Lazy DiscordBot discordBot, GameDao gameDao, ChallengeDao challengeDao) throws IOException {
        this.bot = discordBot;
        this.gameDao = gameDao;
        this.challengeDao = challengeDao;
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

        Optional<Challenge> challenge = challengeDao.findById(String.format("%s-%s-%s", channelId, challengerId, acceptingPlayerId));
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
}
