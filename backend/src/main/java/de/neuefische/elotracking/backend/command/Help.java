package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.Game;
import discord4j.core.object.entity.Message;

import java.util.Optional;

public class Help extends Command {

    public Help(Message msg) {
        super(msg);
    }

    public static String getDescription() {
        return "!help - display this message";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        Optional<Game> game = service.findGameByChannelId(this.channelId);
        String prefix = game.isPresent() ? game.get().getCommandPrefix() : defaultCommandPrefix;
        addBotReply(String.format("Bot commands are: ([] denotes something is optional)\n" +
                        "%s%s\n%s%s\n%s%s\n%s%s\n%s%s\n%s%s\n%s%s",
                prefix, Register.getDescription().substring(1),
                prefix, Challenge.getDescription().substring(1),
                prefix, Accept.getDescription().substring(1),
                prefix, Report.getDescription().substring(1),
                prefix, Report.getDescription2().substring(1),
                prefix, SetPrefix.getDescription().substring(1),
                prefix, Help.getDescription().substring(1)));
    }
}
