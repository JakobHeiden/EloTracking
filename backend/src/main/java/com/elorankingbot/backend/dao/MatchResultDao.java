package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.MatchResult;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchResultDao extends MongoRepository<MatchResult, Long> {
	//void deleteAllByGuildId(long guildId);
	//Optional<Match> findFirstByGuildIdAndWinnerIdAndLoserIdOrderByDate(long guildId, long winnerId, long loserId);
	//List<Match> findAllByGuildIdAndWinnerId(long guildId, long winnerId);
	//List<Match> findAllByGuildIdAndLoserId(long guildId, long loserId);
}
