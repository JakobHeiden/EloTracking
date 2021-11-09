package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Challenge extends Command {

    public Challenge(Message msg) {
        super(msg);
        needsRegisteredChannel = true;
        needsMention = true;
        cantHaveTwoMentions = true;
    }

    public static String getDescription() {
        return "!ch[allenge] @player - challenge another player to a match";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        String challengerId = msg.getAuthor().get().getId().asString();
        String acceptorId = msg.getUserMentionIds().iterator().next().asString();
        if (service.challengeExistsById(channelId + "-" + challengerId + "-" + acceptorId)) {
            addBotReply("challenge already exists");
            canExecute = false;
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(channelId, challengerId);
        ChallengeModel challengeModel = new ChallengeModel(channelId, challengerId, acceptorId);
        service.addChallenge(challengeModel, msg.getId().asString());
        addBotReply(String.format("Challenge issued. Your opponent can now %saccept", service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX")));
    }
}
