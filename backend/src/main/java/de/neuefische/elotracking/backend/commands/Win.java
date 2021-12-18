package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.message.ReactionAddEvent;

public class Win extends Command {

    public Win(ReactionAddEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(event, service, bot, queue);
    }

    @Override
    public void execute() {

    }
}
