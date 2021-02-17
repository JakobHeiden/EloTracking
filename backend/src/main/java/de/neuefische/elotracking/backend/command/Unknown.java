package de.neuefische.elotracking.backend.command;

import discord4j.core.object.entity.Message;

public class Unknown extends Command {

    public Unknown(Message message) {
        super(message);
    }

    @Override
    public void execute() {
        String commandString = msg.getContent().substring(1).split(" ")[0].toLowerCase();
        this.botReplies.add(String.format("Unknown Command %s", commandString));
    }
}
