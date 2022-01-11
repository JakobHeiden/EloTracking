package com.elorankingbot.backend.command;

import discord4j.core.object.component.Button;

public class Buttons {

	// Challenge
	public static Button accept(long channelId) {
		return Button.primary("accept:" + channelId,
				Emojis.checkMark, "Accept");// TODO evtl U+2694 crossed swords?
	}

	public static Button decline(long channelId) {
		return Button.danger("decline:" + channelId,
				Emojis.crossMark, "Decline");
	}

	public static Button win(long channelId) {
		return Button.primary("win:" + channelId,
				Emojis.arrowUp, "Win");
	}

	public static Button lose(long channelId) {
		return Button.primary("lose:" + channelId,
				Emojis.arrowDown, "Lose");
	}

	public static Button draw(long channelId) {
		return Button.primary("draw:" + channelId,
				Emojis.leftRightArrow, "Draw");
	}

	public static Button cancel(long channelId) {
		return Button.danger("cancel:" + channelId,
				Emojis.crossMark, "Cancel match");
	}

	public static Button redo(long channelId) {
		return Button.primary("redo:" + channelId,
				Emojis.redoArrow, "Call for a redo");
	}

	public static Button cancelOnConflict(long channelId) {
		return Button.danger("cancelonconflict:" + channelId,
				Emojis.crossMark, "Cancel match");
	}

	public static Button redoOrCancelOnConflict(long channelId) {
		return Button.primary("redoorcancel:" + channelId,
				Emojis.shrug, "Redo or Cancel");
	}

	public static Button agreeToRedo(long channelId) {
		return Button.primary("redo:" + channelId,
				Emojis.redoArrow, "Agree to a redo");
	}

	public static Button agreeToCancelOnConflict(long channelId) {
		return Button.danger("cancelonconflict:" + channelId,
				Emojis.crossMark, "Cancel match");
	}

	public static Button dispute(long channelId) {
		return Button.secondary("dispute:" + channelId,
				Emojis.exclamation, "File a dispute");
	}

	// Dispute
	public static Button ruleAsWin(long challengeId, boolean isChallengerWin, String winnerName,
								   long challengerChannelId, long acceptorChannelId) {
		return Button.primary(String.format("ruleaswin:%s:%s:%s:%s",
						challengeId, String.valueOf(isChallengerWin),
						challengerChannelId, acceptorChannelId),
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
