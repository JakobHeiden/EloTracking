package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import de.neuefische.elotracking.backend.timedtask.TimedTaskType;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Challenge extends Command {

    public Challenge(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(msg, service, bot, queue);
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
        if (challengerId.equals(acceptorId)) {
            addBotReply("You cannot challenge yourself");
            canExecute = false;
        }
        if (service.challengeExistsById(ChallengeModel.generateId(channelId, challengerId, acceptorId))) {
            addBotReply("challenge already exists");
            canExecute = false;
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(channelId, challengerId);
        ChallengeModel challenge = new ChallengeModel(channelId, challengerId, acceptorId);
        queue.addTimedTask(TimedTaskType.OPEN_CHALLENGE_DECAY, game.getOpenChallengeDecayTime(), channelId);
        service.saveChallenge(challenge);
        addBotReply(String.format("Challenge issued. Your opponent can now %saccept", defaultCommandPrefix));
    }
}
