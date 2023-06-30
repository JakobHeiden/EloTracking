package com.elorankingbot.dao;

import com.elorankingbot.logging.BotStats;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;

public interface BotStatsDao extends MongoRepository<BotStats, Date> {
}
