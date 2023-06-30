package com.elorankingbot.logging;

import com.elorankingbot.model.MatchResult;
import com.elorankingbot.model.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.*;

@Data
@AllArgsConstructor(onConstructor=@__({@PersistenceConstructor}))
public class BotStatsAccumulator {

	public static final String SINGLETON_ID = "singleton";
	public String _id;
	private Map<Long, Integer> serverIdToNumMatches;
	private Set<UUID> playerIds;

	public BotStatsAccumulator() {
		this._id = SINGLETON_ID;
		this.serverIdToNumMatches = new HashMap<>();
		this.playerIds = new HashSet<>();
	}

	public void addMatchResult(MatchResult matchResult) {
		long guildId = matchResult.getServer().getGuildId();
		Integer numMatchesOrNull = serverIdToNumMatches.get(guildId);
		serverIdToNumMatches.put(guildId, numMatchesOrNull == null ? 1 : numMatchesOrNull + 1);
		playerIds.addAll(matchResult.getPlayers().stream().map(Player::getId).toList());
	}

	public int getNumMatches() {
		return serverIdToNumMatches.values().stream().mapToInt(Integer::intValue).sum();
	}
}
