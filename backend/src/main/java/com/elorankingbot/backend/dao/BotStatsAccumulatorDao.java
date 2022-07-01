package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.BotStatsAccumulator;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BotStatsAccumulatorDao extends MongoRepository<BotStatsAccumulator, String> {
}
