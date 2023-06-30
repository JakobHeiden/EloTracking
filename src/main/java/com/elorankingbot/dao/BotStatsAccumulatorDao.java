package com.elorankingbot.dao;

import com.elorankingbot.logging.BotStatsAccumulator;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BotStatsAccumulatorDao extends MongoRepository<BotStatsAccumulator, String> {
}
