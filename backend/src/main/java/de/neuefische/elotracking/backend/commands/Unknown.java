package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;

public class Unknown extends Command {

    public Unknown(Message msg, EloTrackingService service, DiscordBotService bot) {
        super(msg, service, bot);
    }

    @Override
    public void execute() {
        String commandString = msg.getContent().substring(1).split(" ")[0].toLowerCase();
        addBotReply(String.format("Unknown Command %s", commandString));
    }
}
