package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.Game;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Value;

public class Setchallengedecay extends Command {

	@Value("${number-of-time-slots}")
	private int numberOfTimeSlots;

	public Setchallengedecay(Message msg) {
		super(msg);
		this.needsRegisteredChannel = true;
	}

	public static String getDescription() {
		return "!setchallengedecay x - Set open challenges to decay after x minutes";
	}

	public void execute() {
		if (!super.canExecute()) return;

		String xToken = msg.getContent().split(" ")[1];
		int x;
		try {
			x = Integer.valueOf(xToken);
		} catch (NumberFormatException e) {
			addBotReply("You need to specify an integer");
			return;
		}

		if (x <= 0) {
			addBotReply("You need to specify an integer larger than zero");
			return;
		}
		if (x >= numberOfTimeSlots) {
			addBotReply(String.format("This value cannot exceed %s", numberOfTimeSlots - 1));
			return;
		}

		Game game = service.findGameByChannelId(this.channelId).get();
		game.setChallengeDecayTime(x);
		service.saveGame(game);
		addBotReply(String.format("Open challenges are now set to decay after %s minutes.", x));
	}
}
