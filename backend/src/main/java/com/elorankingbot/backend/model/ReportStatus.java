package com.elorankingbot.backend.model;

import com.elorankingbot.backend.tools.Emojis;
import discord4j.core.object.reaction.ReactionEmoji;

// referred to as "ResultStatus" if the match is already done
// TODO evtl doch ResultStatus wieder einfuehren?
public enum ReportStatus {

	NOT_YET_REPORTED(Emojis.notYetReported, -1, null, null),
	WIN(Emojis.win, 1, "won", "Win"),
	LOSE(Emojis.loss, 0, "lost", "Loss"),
	DRAW(Emojis.draw, .5, "drew", "Draw"),
	CANCEL(Emojis.crossMark, -1, null, null);

	private final ReactionEmoji emoji;
	private final double value;
	private final String asVerb;
	private final String asNoun;

	ReportStatus(ReactionEmoji emoji, double value, String asVerb, String asNoun) {
		this.emoji = emoji;
		this.value = value;
		this.asVerb = asVerb;
		this.asNoun = asNoun;
	}

	public ReactionEmoji getEmoji() {
		return this.emoji;
	}

	public String getEmojiAsString() {
		return getEmoji().asUnicodeEmoji().get().getRaw();
	}

	public double getValue() {
		return value;
	}

	public String asVerb() {
		return asVerb;
	}

	public String asNoun() {
		return asNoun;
	}
}
