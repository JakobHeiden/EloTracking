package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;

import java.util.Optional;

public class Accept extends Command {
    public Accept(DiscordBot bot, EloTrackingService service, Message msg, Channel channel) {
        super(bot, service, msg, channel);
        this.needsRegisteredChannel = true;
        this.needsUserTag = true;
    }

    public static String getDescription() {
        return "!accept [@player] - accept an open challenge";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String channelId = channel.getId().asString();
        String acceptingPlayerId = msg.getAuthor().get().getId().asString();
        String challengerId = msg.getUserMentionIds().iterator().next().asString();
        Optional<ChallengeModel> challenge = service.findChallenge(ChallengeModel.generateId(channelId, challengerId, acceptingPlayerId));
        if (challenge.isEmpty()) {
            botReplies.add("No unanswered challenge by that player");
            canExecute = false;
        }
        if (challenge.get().isAccepted()) {
            botReplies.add("already accepted");
            canExecute = false;
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(channelId, acceptingPlayerId);

        challenge.get().accept();
        service.saveChallenge(challenge.get());
        botReplies.add(String.format("Challenge accepted! Come back and %sreport when your game is finished.",
                service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX")));
    }
}
