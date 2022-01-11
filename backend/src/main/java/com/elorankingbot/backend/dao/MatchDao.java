package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchDao extends MongoRepository<Match, Long> {
	void deleteAllByGuildId(long guildId);
}
