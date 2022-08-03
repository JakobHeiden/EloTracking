package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface MatchResultDao extends MongoRepository<MatchResult, UUID> {

	void deleteAllByServerAndGameName(Server server, String gameName);
	void deleteAllByServer(Server server);
}
