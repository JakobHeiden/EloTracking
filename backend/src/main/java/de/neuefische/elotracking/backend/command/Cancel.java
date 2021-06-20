package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Cancel extends Command {

    private boolean canExecute = true;
    private Message msg;

    public Cancel(Message msg) {
        super(msg);
        this.msg = msg;
        this.needsRegisteredChannel = true;
        this.cantHaveTwoMentions = true;
    }

    // For unit testing purposes
    Cancel(Message msg, EloTrackingService service, DiscordBotService bot) {
        this(msg);
        this.service = service;
        this.bot = bot;
    }

    public static String getDescription() {
        return "!cancel [player] - Cancel a challenge to a player or a match with a player";
    }

    public void execute() {
        canExecute = super.canExecute();
        if (!canExecute) return;

        String cancelingPlayerId = msg.getAuthor().get().getId().asString();
        List<ChallengeModel> challenges = service.findAllChallengesForPlayerForChannel(cancelingPlayerId, channelId);
        Optional<Snowflake> mention = msg.getUserMentionIds().stream().findAny();

        ChallengeModel challenge = null;
        if (mention.isEmpty()) {
            return;
            //challenge = inferRelevantChallenge(challenges);
        } else {
           challenge = getRelevantChallenge(challenges, mention.get());
        }
        if (!canExecute) return;

        challenge.callForCancel(cancelingPlayerId);

        if (challenge.shouldBeDeleted()) {
            service.deleteChallenge(challenge);
            addBotReply("Challenge has been deleted.");
            return;
        }

        service.saveChallenge(challenge);
        addBotReply("Your cancellation request has been filed. Your opponent needs to cancel as well for it to take effect");
    }

    private ChallengeModel getRelevantChallenge(List<ChallengeModel> challenges, Snowflake mention) {
        Optional<ChallengeModel> optionalChallenge = challenges.stream().
                filter(chlng -> chlng.getChallengerId().equals(mention.asString()))
                .findAny();

        if (optionalChallenge.isEmpty()) {
            addBotReply("That player has not challenged you");
            canExecute = false;
            return null;
        }

        return optionalChallenge.get();
    }

    private ChallengeModel inferRelevantChallenge(List<ChallengeModel> challenges) {
        return null;
    }
}
