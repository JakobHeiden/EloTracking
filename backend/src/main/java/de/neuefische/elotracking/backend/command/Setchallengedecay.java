package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.Game;
import discord4j.core.object.entity.Message;

public class Setchallengedecay extends Command {

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
			addBotReply("You need to specify a number");
			return;
		}

		Game game = service.findGameByChannelId(this.channelId).get();
		game.setChallengeDecayInMinutes(x);
		service.saveGame(game);
		addBotReply(String.format("Open challenges are now set to decay after %s minutes.", x));
	}
}
