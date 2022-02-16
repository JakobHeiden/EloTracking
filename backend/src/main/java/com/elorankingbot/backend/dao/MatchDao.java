package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MatchDao extends MongoRepository<Match, Long> {
	//void deleteAllByGuildId(long guildId);
	//Optional<Match> findFirstByGuildIdAndWinnerIdAndLoserIdOrderByDate(long guildId, long winnerId, long loserId);
	//List<Match> findAllByGuildIdAndWinnerId(long guildId, long winnerId);
	//List<Match> findAllByGuildIdAndLoserId(long guildId, long loserId);
}
