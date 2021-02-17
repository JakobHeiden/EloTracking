package de.neuefische.elotracking.backend.command;

import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Challenge extends Command {

    public Challenge(Message msg) {
        super(msg);
        needsRegisteredChannel = true;
        needsUserTag = true;
    }

    public static String getDescription() {
        return "!ch[allenge] @player - challenge another player to a match";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String challengerId = msg.getAuthor().get().getId().asString();
        String recipientId = msg.getUserMentionIds().iterator().next().asString();
        if (service.challengeExistsById(channelId + "-" + challengerId + "-" + recipientId)) {
            botReplies.add("challenge already exists");
            canExecute = false;
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(channelId, challengerId);
        service.addChallenge(channelId, challengerId, recipientId);
        botReplies.add(String.format("Challenge issued. Your opponent can now %saccept", service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX")));
    }
}
