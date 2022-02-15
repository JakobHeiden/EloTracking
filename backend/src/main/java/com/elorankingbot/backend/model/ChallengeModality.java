package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@UseToStringForLogging
@Document(collection = "challengemodality")
public class ChallengeModality extends MatchFinderModality {

	private int openChallengeDecayTime;

	public ChallengeModality(String name, Game game) {
		super(name, game);
		this.openChallengeDecayTime = 2 * 60;
	}
}
