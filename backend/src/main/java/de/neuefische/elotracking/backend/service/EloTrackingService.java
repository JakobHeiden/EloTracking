package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.dao.MongoDbDao;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Dummy;
import discord4j.core.object.entity.channel.PrivateChannel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tinylog.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Service
public class EloTrackingService {
    private final DiscordBot discordBot;
    private final MongoDbDao mongoDbDao;
    @Getter
    private Properties config = new Properties();
    private PrivateChannel adminDm;

    @Autowired
    public EloTrackingService(DiscordBot discordBot, MongoDbDao mongoDbDao) throws IOException {
        this.discordBot = discordBot;
        this.mongoDbDao = mongoDbDao;
        config.load(new FileReader("backend/src/main/resources/config.txt"));
        Logger.info(config.getProperty("ADMIN_DISCORD_ID"));
    }

    //TODO remove later on
    public Dummy ping(String data) {
        return mongoDbDao.save(new Dummy(data));
    }
}
