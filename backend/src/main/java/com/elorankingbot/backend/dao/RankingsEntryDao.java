package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.RankingsEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RankingsEntryDao extends MongoRepository<RankingsEntry, UUID> {

	List<RankingsEntry> getAllByGuildIdAndAndGameName(long guildId, String gameName);
	Optional<RankingsEntry> findByGuildIdAndGameNameAndPlayerTag(long guildId, String gameName, String playerTag);
	void deleteAllByGuildIdAndAndGameName(long guildId, String gameName);
}
