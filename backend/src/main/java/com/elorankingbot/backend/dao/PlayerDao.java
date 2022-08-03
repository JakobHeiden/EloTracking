package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerDao extends MongoRepository<Player, UUID> {

	List<Player> findAllByGuildId(long guildId);
	void deleteAllByGuildId(long guildId);
}
