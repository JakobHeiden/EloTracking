package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MatchDao extends MongoRepository<Match, Long> {
	void deleteAllByGuildId(long guildId);
	Optional<Match> findFirstByWinnerIdAndLoserIdOrderByDate(long winnerId, long loserId);
	List<Match> findAllByWinnerIdAndGuildId(long winnerId, long guildId);
	List<Match> findAllByLoserIdAndGuildId(long loserId, long guildId);
}
