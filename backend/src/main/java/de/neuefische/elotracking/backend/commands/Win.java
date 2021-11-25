package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;

public class Win extends Report {

    public Win(Message msg, EloTrackingService service, DiscordBotService bot) {
        super(msg, service, bot, ChallengeModel.ReportStatus.WIN);
    }
}
