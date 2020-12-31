package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Challenge extends Command {
    public static String getDescription() {
        return "!challenge @player - challenge another player to a match";
    }

    public Challenge(DiscordBot bot, EloTrackingService service, Message msg, Channel channel) {
        super(bot, service, msg, channel);
        needsRegisteredChannel = true;
        needsUserTag = true;
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String channelId = channel.getId().asString();
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
