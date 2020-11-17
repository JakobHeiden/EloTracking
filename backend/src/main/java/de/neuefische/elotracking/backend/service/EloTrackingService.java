package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.dao.MongoDbDao;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Dummy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EloTrackingService {
    private final DiscordBot discordBot;
    private final MongoDbDao mongoDbDao;

    @Autowired
    public EloTrackingService(DiscordBot discordBot, MongoDbDao mongoDbDao) {
        this.discordBot = discordBot;
        this.mongoDbDao = mongoDbDao;
        mongoDbDao.save(new Dummy("hallo"));
        System.out.println("hallo?");
    }
}
