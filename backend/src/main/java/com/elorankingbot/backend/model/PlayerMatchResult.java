package com.elorankingbot.backend.model;

import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.UUID;

public class PlayerMatchResult {

	@DBRef(lazy = true)
	private MatchResult matchResult;
	@DBRef(lazy = true)
	private Rating rating;

	private ResultStatus result;
	private double oldRating;
	private double newRating;
	// redundant
	private String tag;
}
