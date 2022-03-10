package com.elorankingbot.backend.tools;

import discord4j.core.object.component.Button;

import java.util.UUID;

public class Buttons {

	// Challenge
	public static Button accept(UUID matchId) {
		return Button.primary("accept:" + matchId.toString(),
				Emojis.checkMark, "Accept");// TODO evtl U+2694 crossed swords?
	}

	public static Button decline(UUID matchId) {
		return Button.danger("decline:" + matchId.toString(),
				Emojis.crossMark, "Decline");
	}

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
		return Button.danger("cancel:" + matchId.toString(),
				Emojis.crossMark, "Cancel match");
	}

	public static Button redo(UUID matchId) {
		return Button.primary("redo:" + matchId.toString(),
				Emojis.redoArrow, "Call for a redo");
	}

	public static Button cancelOnConflict(UUID matchId) {
		return Button.danger("cancelonconflict:" + matchId.toString(),
				Emojis.crossMark, "Call for a cancel");
	}

	public static Button redoOrCancelOnConflict(UUID matchId) {
		return Button.primary("redoorcancel:" + matchId.toString(),
				Emojis.shrug, "Redo or Cancel");
	}

	public static Button agreeToRedo(UUID matchId) {
		return Button.primary("redo:" + matchId.toString(),
				Emojis.redoArrow, "Agree to a redo");
	}

	public static Button agreeToCancelOnConflict(UUID matchId) {
		return Button.danger("cancelonconflict:" + matchId.toString(),
				Emojis.crossMark, "Agree to a cancel");
	}

	public static Button dispute(UUID matchId) {
		return Button.secondary("dispute:" + matchId.toString(),
				Emojis.exclamation, "File a dispute");
	}

	// Dispute
	public static Button ruleAsWin(long challengeId, boolean isChallengerWin, String winnerName) {
		return Button.primary(String.format("ruleaswin:%s:%s",
						challengeId, isChallengerWin),
				Emojis.win, "Rule the match a win for " + winnerName);
	}

	public static Button ruleAsDraw(long challengeId) {
		return Button.primary("ruleasdraw:" + challengeId,
				Emojis.draw, "Rule the match a draw");
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
