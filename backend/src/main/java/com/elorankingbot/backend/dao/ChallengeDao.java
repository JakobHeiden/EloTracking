package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.ChallengeModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeDao extends MongoRepository<ChallengeModel, Long> {
    List<ChallengeModel> findAllByGuildIdAndAcceptorId(long guildId, long acceptorId);
    List<ChallengeModel> findAllByGuildIdAndChallengerId(long guildId, long challengerId);
	void deleteAllByGuildId(long guildId);
}
