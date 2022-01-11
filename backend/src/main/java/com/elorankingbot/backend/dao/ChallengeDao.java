package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.ChallengeModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeDao extends MongoRepository<ChallengeModel, Long> {
    List<ChallengeModel> findAllByAcceptorId(long acceptorId);
    List<ChallengeModel> findAllByChallengerId(long challengerId);
    Boolean existsByChallengerMessageId(long challengerMessageId);
    Optional<ChallengeModel> findByChallengerMessageId(long challengerMessageId);
    Boolean existsByAcceptorMessageId(long acceptorMessageId);
    Optional<ChallengeModel> findByAcceptorMessageId(long acceptorMessageId);
	void deleteAllByGuildId(long guildId);
}
