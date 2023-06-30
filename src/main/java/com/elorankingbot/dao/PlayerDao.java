package com.elorankingbot.dao;

import com.elorankingbot.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerDao extends MongoRepository<Player, UUID> {

	List<Player> findAllByGuildId(long guildId);
	void deleteAllByGuildId(long guildId);
}
