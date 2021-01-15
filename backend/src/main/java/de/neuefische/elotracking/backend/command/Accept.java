package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Accept extends Command {
    public Accept(DiscordBot bot, EloTrackingService service, Message msg, Channel channel) {
        super(bot, service, msg, channel);
        this.needsRegisteredChannel = true;
    }

    public static String getDescription() {
        return "!ac[cept] [@player] - accept an open challenge";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        String acceptingPlayerId = msg.getAuthor().get().getId().asString();
        String channelId = channel.getId().asString();

        Optional<ChallengeModel> inferredChallenge = inferRelevantChallenge(msg.getUserMentionIds(), acceptingPlayerId, channelId);
        if (inferredChallenge.isEmpty()) {
            return;
        }
        ChallengeModel challenge = inferredChallenge.get();

        //Challenger is determined and the challenge not yet accepted, so proceed
        service.addNewPlayerIfPlayerNotPresent(channelId, acceptingPlayerId);
        challenge.accept();
        service.saveChallenge(challenge);
        botReplies.add(String.format("Challenge accepted! Come back and %sreport when your game is finished.",
                service.findGameByChannelId(channelId).get().getCommandPrefix()));
    }

    private Optional<ChallengeModel> inferRelevantChallenge(Set<Snowflake> mentions, String acceptingPlayerId, String channelId) {
        boolean canInfer = true;
        List<ChallengeModel> challenges = service.findChallengesOfPlayerForChannel(acceptingPlayerId, channelId);
        int numMentions = mentions.size();

        //check if the challenge was already accepted
        if (numMentions == 1) {
            for (ChallengeModel challenge : challenges) {
                if (challenge.getChallengerId().equals(mentions.iterator().next())) {
                    botReplies.add("Challenge already accepted");
                    return Optional.empty();
                }
            }
        }

        //accepted challenges of other players aren't relevant anymore
        challenges.removeIf(ChallengeModel::isAccepted);

        //Rule out some states that won't allow for inference
        if (challenges.size() == 0) {
            botReplies.add("No open challenge present against you");
            canInfer = false;
        }
        if (numMentions == 0 && challenges.size() > 1) {
            botReplies.add("There are several open challenges present against you. Please specify which you want to accept: " +
                    getChallengerNames(challenges));
            canInfer = false;
        }
        if (!canInfer) return Optional.empty();

        //some possible inference scenarios
        if (numMentions == 1) {
            String challengerId = mentions.iterator().next().asString();
            Optional<ChallengeModel> challengeMatchingMention = Optional.empty();
            for (ChallengeModel challenge : challenges) {
                if (challenge.getChallengerId().equals(challengerId)) {
                    challengeMatchingMention = Optional.of(challenge);
                }
            }
            if (challengeMatchingMention.isEmpty()) {
                botReplies.add("No challenge present from that player. There are open challenges from: " +
                        getChallengerNames(challenges));
            }
            return challengeMatchingMention;
        }

        //only possible state left is 1 challenge, 0 mentions
        return Optional.of(challenges.iterator().next());
    }

    private String getChallengerNames(List<ChallengeModel> challenges) {
        if (challenges.isEmpty()) return "";

        String returnString = "";
        for (ChallengeModel challenge : challenges) {//TODO make requests run parralel
            returnString += String.format("%s, ", bot.getPlayerName(challenge.getChallengerId()));
        }
        return returnString.substring(0, returnString.length() - 2);
    }
}
