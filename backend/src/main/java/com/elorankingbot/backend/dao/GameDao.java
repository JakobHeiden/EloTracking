package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameDao extends MongoRepository<Game, Long> {
}
