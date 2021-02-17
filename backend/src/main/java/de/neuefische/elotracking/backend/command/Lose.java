package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import discord4j.core.object.entity.Message;

public class Lose extends Report {

    public Lose(Message message) {
        super(message, ChallengeModel.ReportStatus.LOSS);
    }
}
