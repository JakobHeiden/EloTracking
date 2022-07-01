package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.BotStats;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;

public interface BotStatsDao extends MongoRepository<BotStats, Date> {
}
