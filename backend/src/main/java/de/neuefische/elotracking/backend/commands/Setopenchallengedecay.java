package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;

public class Setopenchallengedecay extends Settime {

	public Setopenchallengedecay(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(msg, service, bot, queue);
	}

	public static String getDescription() {
		return "!setopenchallengedecay x - Set open challenges to decay after x minutes";
	}

	public void execute() {
		if (!super.canExecute()) return;

		game.setOpenChallengeDecayTime(time);
		service.saveGame(game);
		addBotReply(String.format("Open challenges are now set to decay after %s minutes.", time));
	}
}
