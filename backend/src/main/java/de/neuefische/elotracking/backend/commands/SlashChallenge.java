package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlashChallenge extends SlashCommand {

    public SlashChallenge(ChatInputInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(event, service, bot, queue);
    }

    public static String getDescription() {
        return "!ch[allenge] @player - challenge another player to a match";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        String challengerId = event.getInteraction().getUser().getId().asString();
        String acceptorId = event.getOption("player").get().getValue().get().asUser().block().getId().asString();
        if (challengerId.equals(acceptorId)) {
            addBotReply("You cannot challenge yourself");// TODO anders ausschliessen?
            canExecute = false;
        }
        if (service.challengeExistsById(ChallengeModel.generateId(guildId, challengerId, acceptorId))) {
            addBotReply("challenge already exists");
            canExecute = false;
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(guildId, challengerId);
        ChallengeModel challenge = new ChallengeModel(guildId, challengerId, acceptorId);
        queue.addTimedTask(TimedTask.TimedTaskType.OPEN_CHALLENGE_DECAY, game.getOpenChallengeDecayTime(), guildId);
        service.saveChallenge(challenge);
        addBotReply(String.format("Challenge issued. Your opponent can now %saccept", defaultCommandPrefix));
    }
}
