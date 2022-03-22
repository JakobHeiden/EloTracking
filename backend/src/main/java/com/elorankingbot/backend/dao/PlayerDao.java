package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface PlayerDao extends MongoRepository<Player, UUID> {
}
