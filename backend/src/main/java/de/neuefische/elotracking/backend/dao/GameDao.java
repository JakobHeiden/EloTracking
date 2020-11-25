package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameDao extends MongoRepository<Game, String> {
    public Game findByChannelId(String channelId);
    public boolean existsByChannelId(String channelId);
}
