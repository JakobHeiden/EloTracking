package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
public class Cancel extends Command {

    private boolean canExecute = true;
    private Message msg;
    private String cancelingPlayerId;

    public Cancel(Message msg) {
        super(msg);
        this.msg = msg;
        this.needsRegisteredChannel = true;
        this.cantHaveTwoMentions = true;
    }

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

        cancelingPlayerId = msg.getAuthor().get().getId().asString();
        List<ChallengeModel> challenges = service.findAllChallengesForPlayerForChannel(cancelingPlayerId, channelId);
        Optional<Snowflake> mention = msg.getUserMentionIds().stream().findAny();

        ChallengeModel challenge = null;
        if (mention.isEmpty()) {
            challenge = inferRelevantChallenge(challenges);
        } else {
            challenge = getRelevantChallenge(challenges, mention.get());
        }
        if (!canExecute) return;

        challenge.callForCancel(cancelingPlayerId);

        if (challenge.shouldBeDeleted()) {
            service.deleteChallenge(challenge.getId());
            addBotReply("Challenge has been deleted.");
            return;
        }

        service.saveChallenge(challenge);
        addBotReply("Your cancellation request has been filed. Your opponent needs to cancel as well for it to take effect");
    }

    private ChallengeModel getRelevantChallenge(List<ChallengeModel> challenges, Snowflake mention) {
        Optional<ChallengeModel> optionalChallenge = challenges.stream().
                filter(chlng -> chlng.getChallengerId().equals(mention.asString()) || chlng.getAcceptorId().equals(mention.asString()))
                .findAny();

        if (optionalChallenge.isEmpty()) {
            addBotReply("That player has not challenged you");
            canExecute = false;
            return null;
        }

        return optionalChallenge.get();
    }

    private ChallengeModel inferRelevantChallenge(List<ChallengeModel> challenges) {
        List<ChallengeModel> challengesOfPlayer = challenges.stream()
                .filter(chlng -> chlng.getChallengerId().equals(cancelingPlayerId) || chlng.getAcceptorId().equals(cancelingPlayerId))
                .collect(toUnmodifiableList());

        if (challengesOfPlayer.size() == 0) {
            addBotReply("No challenge present that could be canceled");
            canExecute = false;
            return null;
        }
        if (challengesOfPlayer.size() > 1) {
            addBotReply("More than one possible challenge to cancel, please specify...");
            canExecute = false;
            return null;
        }

        return challengesOfPlayer.get(0);
    }
}
