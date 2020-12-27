package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Challenge extends Command {
    public static String getDescription() {
        return "challenge command";
    }

    public Challenge(DiscordBot bot, EloTrackingService service, Message msg, Channel channel) {
        super(bot, service, msg, channel);
        needsRegisteredChannel = true;
        needsUserTag = true;
    }

    public void execute() {
        super.determineIfCanExecute();
        if (!canExecute) { return; }

        String otherPlayerId = msg.getUserMentionIds().iterator().next().asString();
        String replyFromService = service.challenge(
                channel.getId().asString(),
                msg.getAuthor().get().getId().asString(),
                otherPlayerId);
        log.debug("replyFromService is " + replyFromService);
        botReplies.add(replyFromService);
    }
}
