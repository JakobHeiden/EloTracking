package com.elorankingbot.model;

import com.elorankingbot.components.Emojis;
import discord4j.core.object.reaction.ReactionEmoji;

// referred to as "ResultStatus" if the match is already done
// TODO evtl doch ResultStatus wieder einfuehren?
public enum ReportStatus {

	NOT_YET_REPORTED(Emojis.notYetReported, -1, null, null, null),
	WIN(Emojis.win, 1, "won", "defeated", "win"),// TODO das emoji ueber den value holen?
	LOSE(Emojis.loss, 0, "lost", "lost to", "loss"),// noun vllt auch ueber value?
	DRAW(Emojis.draw, .5, "drew", "drew", "draw"),
	CANCEL(Emojis.crossMark, -1, null, "canceled with", "cancel");

	public final ReactionEmoji emoji;
	public final double value;
	public final String asVerb;
	public final String asRelationalVerb;
	public final String asNoun;

	ReportStatus(ReactionEmoji emoji, double value, String asVerb, String asRelationalVerb, String asNoun) {
		this.emoji = emoji;
		this.value = value;
		this.asVerb = asVerb;
		this.asRelationalVerb = asRelationalVerb;
		this.asNoun = asNoun;
	}

	public String asEmojiAsString() {
		return emoji.asUnicodeEmoji().get().getRaw();
	}

	public String asCapitalizedNoun() {
		return asNoun.substring(0, 1).toUpperCase() + asNoun.substring(1);
	}
}
