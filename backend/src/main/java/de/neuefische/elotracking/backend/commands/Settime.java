package de.neuefische.elotracking.backend.commands;

import com.google.common.primitives.Ints;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;

public abstract class Settime extends Command {

	protected int numberOfTimeSlots;
	protected int time;

	protected Settime(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(msg, service, bot, queue);
		this.needsRegisteredChannel = true;
		this.numberOfTimeSlots = service.getPropertiesLoader().getNumberOfTimeSlots();
	}

	protected boolean canExecute() {
		if (!super.canExecute()) return false;

		if (msg.getContent().split(" ").length == 1) {
			addBotReply("Please specify an integer");
			return false;
		}

		String xToken = msg.getContent().split(" ")[1];
		Integer maybeTime = Ints.tryParse(xToken);
		if (maybeTime == null) {
			addBotReply("You need to specify an integer");
			return false;
		}

		time = maybeTime;
		if (time <= 0) {
			addBotReply("You need to specify an integer larger than zero");
			return false;
		}

		if (time >= numberOfTimeSlots) {
			addBotReply(String.format("This value cannot exceed %s", numberOfTimeSlots - 1));
			return false;
		}

		return true;
	}

}
