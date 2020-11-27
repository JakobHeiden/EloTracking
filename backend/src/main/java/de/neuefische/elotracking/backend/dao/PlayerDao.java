package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlayerDao extends MongoRepository<Player, String> {
}
