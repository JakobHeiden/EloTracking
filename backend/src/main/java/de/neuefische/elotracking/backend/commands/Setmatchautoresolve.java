package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;

public class Setmatchautoresolve extends Settime {

	public Setmatchautoresolve(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(msg, service, bot, queue);
	}

	public static String getDescription() {
		return "!setmatchautoresolve x - Set open challenges to decay after x minutes";
	}

	public void execute() {
		if (!super.canExecute()) return;

		game.setMatchAutoResolveTime(time);
		service.saveGame(game);
		addBotReply(String.format("One-sided reports are now set to auto-resolve after %s minutes.", time));
	}
}
