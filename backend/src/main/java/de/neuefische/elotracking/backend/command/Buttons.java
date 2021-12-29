package de.neuefische.elotracking.backend.command;

import discord4j.core.object.component.Button;

public class Buttons {

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

	public static Button accept(long channelId) {
		return Button.primary("accept:" + channelId,
				Emojis.checkMark, "Accept");
	}

	public static Button decline(long channelId) {
		return Button.danger("decline:" + channelId,
				Emojis.crossMark, "Decline");
	}

	public static Button redo(long channelId) {
		return Button.primary("redo:" + channelId,
				Emojis.redoArrow, "Call for a redo");
	}

	public static Button agreeToRedo(long channelId) {
		return Button.primary("redo:" + channelId,
				Emojis.redoArrow, "Agree to a redo");
	}

	public static Button cancelOnConflict(long channelId) {
		return Button.danger("cancelonconflict:" + channelId,
				Emojis.crossMark, "Cancel match");
	}

	public static Button agreeToCancelOnConflict(long channelId) {
		return Button.danger("cancelonconflict:" + channelId,
				Emojis.crossMark, "Cancel match");
	}

	public static Button redoOrCancelOnConflict(long channelId) {
		return Button.primary("redoorcancel:" + channelId,
				Emojis.shrug, "Redo or Cancel");
	}

	public static Button dispute(long channelId) {
		return Button.secondary("dispute:" + channelId,
				Emojis.exclamation, "File a dispute");
	}
}
