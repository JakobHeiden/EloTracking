package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface MatchDao extends MongoRepository<Match, UUID> {
}
