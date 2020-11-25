package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.dao.GameDao;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Game;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tinylog.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Service
public class EloTrackingService {
    private final DiscordBot bot;
    private final GameDao gameDao;
    @Getter
    private Properties config = new Properties();

    @Autowired
    public EloTrackingService(@Lazy DiscordBot discordBot, GameDao gameDao) throws IOException {
        this.bot = discordBot;
        this.gameDao = gameDao;
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
            bot.sendToAdmin(String.format("Insert name Game to db failed: %s %s", channelId, name));
            return String.format("Internal database error. %s please take a look at this", bot.getAdminMentionAsString());
        }

        return String.format(String.format("New game created. You can now %schallenge another player", bot.getPrefix()));
    }
}
