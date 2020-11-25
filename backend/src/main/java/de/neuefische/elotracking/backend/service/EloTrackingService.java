package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.dao.MongoDbDao;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Dummy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Service
public class EloTrackingService {
    private final DiscordBot bot;
    private final MongoDbDao mongoDbDao;
    @Getter
    private Properties config = new Properties();

    @Autowired
    public EloTrackingService(@Lazy DiscordBot discordBot, MongoDbDao mongoDbDao) throws IOException {
        this.bot = discordBot;
        this.mongoDbDao = mongoDbDao;
        config.load(new FileReader("backend/src/main/resources/config.txt"));
    }

    //TODO remove later on
    public Dummy ping(String data) {
        return mongoDbDao.save(new Dummy(data));
    }
}
