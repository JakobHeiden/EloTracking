package com.elorankingbot.model;

import java.util.List;
import java.util.Optional;

public record RankingsExcerpt(
		Game game,
		List<RankingsEntry> rankingsEntries,
		int highestRank,
		Optional<String> tagToHighlight,
		int numTotalPlayers
) {
}
