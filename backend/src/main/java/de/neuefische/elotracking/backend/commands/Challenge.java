package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Challenge extends Command {// TODO! struktur sinnvoll? -> test migrieren

    public Challenge(ApplicationCommandInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(event, service, bot, queue);
    }

    public static String getDescription() {
        return "!ch[allenge] @player - challenge another player to a match";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        long challengerId = event.getInteraction().getUser().getId().asLong();
        long acceptorId = 0L;
        if (event instanceof ChatInputInteractionEvent) {
            acceptorId = ((ChatInputInteractionEvent) event).getOption("player").get().getValue().get().asUser().block().getId().asLong();
        } else if (event instanceof UserInteractionEvent) {
            acceptorId = ((UserInteractionEvent) event).getTargetId().asLong();
        }
        log.warn(String.valueOf(acceptorId));

        if (challengerId == acceptorId) {
            addBotReply("You cannot challenge yourself");// TODO anders ausschliessen?
            canExecute = false;
        }
        if (service.challengeExistsByParticipants(guildId, challengerId, acceptorId)) {
            addBotReply("challenge already exists");
            canExecute = false;
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(guildId, challengerId);
        Message message = bot.sendToUser(acceptorId, String.format("You have been challenged by <@%s>. Accept?", challengerId)).block();
        message.addReaction(bot.checkMark).subscribe();
        message.addReaction(bot.crossMark).subscribe();
        ChallengeModel challenge = new ChallengeModel(guildId, message.getId().asLong(), challengerId, acceptorId);
        queue.addTimedTask(TimedTask.TimedTaskType.OPEN_CHALLENGE_DECAY, game.getOpenChallengeDecayTime(), guildId);
        service.saveChallenge(challenge);
        addBotReply("Challenge issued. Your opponent can now /accept");
    }
}
