package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;

public class Win extends Report {

    public Win(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(msg, service, bot, queue, ChallengeModel.ReportStatus.WIN);
    }
}
