package com.elorankingbot.dao;

import com.elorankingbot.model.Match;
import com.elorankingbot.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface MatchDao extends MongoRepository<Match, UUID> {

	void deleteAllByServerAndGameId(Server server, String gameId);
	void deleteAllByServer(Server server);
	List<Match> findAllByServer(Server server);
}
