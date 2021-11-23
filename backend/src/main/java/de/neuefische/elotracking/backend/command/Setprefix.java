package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.Game;
import discord4j.core.object.entity.Message;

public class Setprefix extends Command {

    public static String getDescription() {
        return "!setprefix x - Change the command prefix for the bot. Only applies to this channel";
    }

    public Setprefix(Message msg) {
        super(msg);
        this.needsRegisteredChannel = true;
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String newPrefix = msg.getContent().substring(1 + "setprefix".length()).trim();
        if (newPrefix.length() != 1 || newPrefix.matches("[a-zA-Z0-9]")) {
            addBotReply("Please specify a single special character");
            canExecute = false;
        }
        if (!canExecute) return;

        Game game = service.findGameByChannelId(this.channelId).get();
        game.setCommandPrefix(newPrefix);
        service.saveGame(game);
        addBotReply(String.format("Command prefix changed to %s", newPrefix));
    }
}
