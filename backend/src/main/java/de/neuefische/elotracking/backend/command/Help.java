package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;

public class Help extends Command {
    public Help(DiscordBot bot, EloTrackingService service, Message msg, Channel channel) {
        super(bot, service, msg, channel);
    }

    public static String getDescription() {
        return "Help command";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        botReplies.add(String.format("%s\n%s\n%s\n%s\n%s",
            Accept.getDescription(),
            Challenge.getDescription(),
            Help.getDescription(),
            Register.getDescription(),
            SetPrefix.getDescription()));
       }
}
