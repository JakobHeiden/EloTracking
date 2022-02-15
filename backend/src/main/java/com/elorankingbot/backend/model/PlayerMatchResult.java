package com.elorankingbot.backend.model;

import java.util.UUID;

public class PlayerMatchResult {

	private UUID playerId;
	private boolean isWin;
	private String tag;
	private double oldRating;
	private double newRating;
}
