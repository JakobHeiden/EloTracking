package com.elorankingbot.backend.tools;

import discord4j.core.object.component.Button;

public class Buttons {

	// Challenge
	public static Button accept(long id) {
		return Button.primary("accept:" + id,
				Emojis.checkMark, "Accept");// TODO evtl U+2694 crossed swords?
	}

	public static Button decline(long id) {
		return Button.danger("decline:" + id,
				Emojis.crossMark, "Decline");
	}

	public static Button win(long id) {
		return Button.primary("win:" + id,
				Emojis.arrowUp, "Win");
	}

	public static Button lose(long id) {
		return Button.primary("lose:" + id,
				Emojis.arrowDown, "Lose");
	}

	public static Button draw(long id) {
		return Button.primary("draw:" + id,
				Emojis.leftRightArrow, "Draw");
	}

	public static Button cancel(long id) {
		return Button.danger("cancel:" + id,
				Emojis.crossMark, "Cancel match");
	}

	public static Button redo(long id) {
		return Button.primary("redo:" + id,
				Emojis.redoArrow, "Call for a redo");
	}

	public static Button cancelOnConflict(long id) {
		return Button.danger("cancelonconflict:" + id,
				Emojis.crossMark, "Call for a cancel");
	}

	public static Button redoOrCancelOnConflict(long id) {
		return Button.primary("redoorcancel:" + id,
				Emojis.shrug, "Redo or Cancel");
	}

	public static Button agreeToRedo(long id) {
		return Button.primary("redo:" + id,
				Emojis.redoArrow, "Agree to a redo");
	}

	public static Button agreeToCancelOnConflict(long id) {
		return Button.danger("cancelonconflict:" + id,
				Emojis.crossMark, "Agree to a cancel");
	}

	public static Button dispute(long id) {
		return Button.secondary("dispute:" + id,
				Emojis.exclamation, "File a dispute");
	}

	// Dispute
	public static Button ruleAsWin(long challengeId, boolean isChallengerWin, String winnerName) {
		return Button.primary(String.format("ruleaswin:%s:%s",
						challengeId, isChallengerWin),
				Emojis.arrowUp, "Rule the match a win for " + winnerName);
	}

	public static Button ruleAsDraw(long challengeId) {
		return Button.primary("ruleasdraw:" + challengeId,
				Emojis.leftRightArrow, "Rule the match a draw");
	}

	public static Button ruleAsCancel(long challengeId) {
		return Button.danger("ruleascancel:" + challengeId,
				Emojis.crossMark, "Rule the match as canceled");
	}

	public static Button closeChannelNow() {
		return Button.secondary("closechannelnow",
				Emojis.crossMark, "Close this channel now");
	}

	public static Button closeChannelLater() {
		return Button.secondary("closechannellater",
				Emojis.hourglass, "Close this channel in 24h");// TODO vllt konfbar machen
	}
}
