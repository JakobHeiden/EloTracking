package com.elorankingbot.components;

import com.elorankingbot.commands.admin.deleteranking.AbortDeleteRanking;
import com.elorankingbot.commands.admin.deleteranking.ConfirmDeleteRanking;
import discord4j.core.object.component.Button;

import java.util.UUID;

public class Buttons {// TODO string ersetzen duch klassennamen

	// Challenge
	public static Button accept(UUID matchId) {
		return Button.primary("accept:" + matchId.toString(),
				Emojis.checkMark, "Accept");// TODO evtl U+2694 crossed swords?
	}

	public static Button decline(UUID matchId) {
		return Button.danger("decline:" + matchId.toString(),
				Emojis.crossMark, "Decline");
	}

	// Match
	public static Button win(UUID matchId) {
		return Button.primary("win:" + matchId.toString(),
				Emojis.win, "Win");
	}

	public static Button lose(UUID matchId) {
		return Button.primary("lose:" + matchId.toString(),
				Emojis.loss, "Lose");
	}

	public static Button draw(UUID matchId) {
		return Button.primary("draw:" + matchId.toString(),
				Emojis.draw, "Draw");
	}

	public static Button cancel(UUID matchId) {
		return Button.secondary("cancel:" + matchId.toString(),
				Emojis.crossMark, "Cancel match");
	}

	public static Button dispute(UUID matchId) {
		return Button.danger("dispute:" + matchId.toString(), "File a dispute");
	}

	// Dispute
	public static Button ruleAsWin(UUID matchId, int teamIndex) {
		return Button.primary(String.format("ruleaswin:%s:%s", matchId, teamIndex),
				Emojis.win, "Rule the match a win for team #" + (teamIndex + 1));
	}

	public static Button ruleAsDraw(UUID matchId) {
		return Button.primary("ruleasdraw:" + matchId,
				Emojis.draw, "Rule the match a draw");
	}

	public static Button ruleAsCancel(UUID matchId) {
		return Button.secondary("ruleascancel:" + matchId,
				Emojis.crossMark, "Rule the match as canceled");
	}

	// DeleteRanking confirmation dialog
	public static Button confirmDeleteRanking(String gameName, long userId) {
		return Button.danger(String.format("%s:%s:%s",
						ConfirmDeleteRanking.class.getSimpleName().toLowerCase(), gameName, userId),
				Emojis.doubleExclamation, "Yes, delete");
	}

	public static Button abortDeleteRanking(String gameName, long userId) {
		return Button.secondary(String.format("%s:%s:%s",
						AbortDeleteRanking.class.getSimpleName().toLowerCase(), gameName, userId),
				Emojis.crossMark, "No, do not delete");
	}

	// Settings


}
