package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import discord4j.core.object.entity.Message;

public class Win extends Report {

    public Win(Message msg) {
        super(msg, ChallengeModel.ReportStatus.WIN);
    }
}
