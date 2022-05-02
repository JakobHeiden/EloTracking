package com.elorankingbot.backend.model;

import com.elorankingbot.backend.components.FormatTools;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;

import static com.elorankingbot.backend.components.FormatTools.formatRating;

@Data
@AllArgsConstructor
public class PlayerMatchResult {

	@DBRef(lazy = true)
	private MatchResult matchResult;
	@DBRef(lazy = true)
	private Player player;
	// redundant
	private String playerTag;
	private ReportStatus resultStatus;
	private double oldRating;
	private double newRating;

	public String getRatingChangeAsString() {
		return FormatTools.formatRatingChange(newRating - oldRating);
	}

	@Override // for logging
	public String toString() {
		return String.format("%s:%s->%s", playerTag, resultStatus.asNoun, formatRating(newRating));
	}
}
