package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;

public class SetPrefix extends Command {
    public static String getDescription() {
        return "!setprefix x - Change the command prefix for the bot. Only applies to this channel";
    }

    public SetPrefix(DiscordBot bot, EloTrackingService service, Message msg) {
        super(bot, service, msg);
        this.needsRegisteredChannel = true;
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String newPrefix = msg.getContent().substring(1 + "setprefix".length()).trim();
        if (newPrefix.length() != 1 || newPrefix.matches("[a-zA-Z0-9]")) {
            botReplies.add("Please specify a single special character");
            canExecute = false;
        }
        if (!canExecute) return;

        Game game = service.findGameByChannelId(this.channelId).get();
        game.setCommandPrefix(newPrefix);
        service.saveGame(game);
        botReplies.add(String.format("Command prefix changed to %s", newPrefix));
    }
}
