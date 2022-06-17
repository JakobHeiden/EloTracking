package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.MatchResultReference;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MatchResultReferenceDao extends MongoRepository<MatchResultReference, Long> {

	Optional<MatchResultReference> findByResultMessageId(long resultMessageId);
	Optional<MatchResultReference> findByMatchMessageId(long matchMessageId);
}
