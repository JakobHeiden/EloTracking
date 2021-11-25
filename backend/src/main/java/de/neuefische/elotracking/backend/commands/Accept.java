package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Accept extends Command {

    private boolean canExecute = true;

    Accept(Message msg, EloTrackingService service, DiscordBotService bot) {
        super(msg, service, bot);
    }

    public static String getDescription() {
        return "!ac[cept] [@player] - accept an open challenge";
    }

    public void execute() {
        if (!super.canExecute()) return;

        String acceptingPlayerId = msg.getAuthor().get().getId().asString();
        List<ChallengeModel> challenges = service.findAllChallengesOfAcceptorForChannel(acceptingPlayerId, channelId);
        Optional<Snowflake> mention = msg.getUserMentionIds().stream().findAny();
        Optional<ChallengeModel> challenge;

        if (mention.isEmpty()) {
            challenge = inferRelevantChallenge(challenges);
        } else {
            challenge = getRelevantChallenge(challenges, mention.get());
        }
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(channelId, acceptingPlayerId);
        challenge.get().accept();// TODO doppelte akzeptierung behandeln
        service.saveChallenge(challenge.get());
        addBotReply(String.format("Challenge accepted! Come back and %sreport when your game is finished.",
                service.findGameByChannelId(channelId).get().getCommandPrefix()));
    }

    private Optional<ChallengeModel> inferRelevantChallenge(List<ChallengeModel> challenges) {
        challenges.removeIf(ChallengeModel::isAccepted);
        if (challenges.size() == 0) {
            addBotReply("No open challenge present against you");
            canExecute = false;
            return Optional.empty();
        }
        if (challenges.size() > 1) {
            addBotReply("There are several open challenges present against you. Please specify which you want to accept: " +
                    getChallengerNames(challenges));
            canExecute = false;
            return Optional.empty();
        }

        return Optional.of(challenges.get(0));
    }

    private Optional<ChallengeModel> getRelevantChallenge(List<ChallengeModel> challenges, Snowflake mention) {
        Optional<ChallengeModel> challenge = challenges.stream().
                filter(chlng -> chlng.getChallengerId().equals(mention.asString()))
                .findAny();

        if (challenge.isEmpty()) {
            addBotReply("That player has not yet challenged you");
            canExecute = false;
            return challenge;
        }
        if (challenge.get().isAccepted()) {
            addBotReply("You already accepted that Challenge");
            canExecute = false;
            return Optional.empty();
        }

        return challenge;
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
