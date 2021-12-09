package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlayerDao extends MongoRepository<Player, String> {
    List<Player> findAllByChannelId(String channelId);
}
