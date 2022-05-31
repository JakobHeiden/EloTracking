package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface MatchDao extends MongoRepository<Match, UUID> {

	void deleteAllByServerAndGameId(Server server, String gameId);
	List<Match> findAllByServer(Server server);
}
