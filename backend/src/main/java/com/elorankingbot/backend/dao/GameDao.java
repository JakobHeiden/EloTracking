package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Ranking;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameDao extends MongoRepository<Ranking, Long> {
}
