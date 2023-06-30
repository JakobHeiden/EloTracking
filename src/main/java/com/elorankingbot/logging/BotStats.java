package com.elorankingbot.logging;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@AllArgsConstructor
public class BotStats {

	@Id
	private String date;
	private int numActiveServers, numActivePlayers, numMatches;

	public static BotStats botStatsOf(BotStatsAccumulator botStatsAccumulator) {
		return new BotStats(new Date().toString(), botStatsAccumulator.getServerIdToNumMatches().size(),
				botStatsAccumulator.getPlayerIds().size(), botStatsAccumulator.getNumMatches());
	}
}
