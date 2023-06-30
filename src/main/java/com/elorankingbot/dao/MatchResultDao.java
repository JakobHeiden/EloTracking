package com.elorankingbot.dao;

import com.elorankingbot.model.MatchResult;
import com.elorankingbot.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface MatchResultDao extends MongoRepository<MatchResult, UUID> {

	void deleteAllByServerAndGameName(Server server, String gameName);
	void deleteAllByServer(Server server);
}
