package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;

public class Setacceptedchallengedecay extends Settime {

	public Setacceptedchallengedecay(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(msg, service, bot, queue);
	}

	public static String getDescription() {
		return "!setacceptedchallengedecay x - Set accepted challenges to decay after x minutes";
	}

	public void execute() {
		if (!super.canExecute()) return;

		game.setAcceptedChallengeDecayTime(time);
		service.saveGame(game);
		addBotReply(String.format("Accepted challenges are now set to decay after %s minutes.", time));
	}
}
