package com.elorankingbot.dao;

import com.elorankingbot.model.RankingsEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RankingsEntryDao extends MongoRepository<RankingsEntry, UUID> {

	List<RankingsEntry> getAllByGuildIdAndAndGameName(long guildId, String gameName);
	List<RankingsEntry> findTopByGuildIdAndGameName(long guildId, String gameName, PageRequest pageRequest);
	Optional<RankingsEntry> findByGuildIdAndGameNameAndPlayerTag(long guildId, String gameName, String playerTag);
	void deleteAllByGuildIdAndAndGameName(long guildId, String gameName);
	void deleteAllByGuildId(long guildId);
}
