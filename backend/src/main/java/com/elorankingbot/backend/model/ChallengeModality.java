package com.elorankingbot.backend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "challengemodality")
public class ChallengeModality {

	private String name;
	@DBRef(lazy = true)
	private Game game;
	private int openChallengeDecayTime;

	public ChallengeModality(String name, Game game) {
		this.game = game;
		this.name = name;
		this.openChallengeDecayTime = 2 * 60;
	}
}
