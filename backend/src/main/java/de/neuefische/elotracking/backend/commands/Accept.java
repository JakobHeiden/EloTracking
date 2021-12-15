package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
@Slf4j
public class Accept extends Command {

    private boolean canExecute = true;

    public Accept(ApplicationCommandInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(event, service, bot, queue);
    }

    public static String getDescription() {
        return "!ac[cept] [@player] - accept an open challenge";
    }

    public void execute() {
        if (!super.canExecute()) return;

        String acceptorId = event.getInteraction().getUser().getId().asString();
        List<ChallengeModel> challenges = service.findAllChallengesByAcceptorIdAndChannelId(acceptorId, guildId);
        String challengerId = null;
        if (event instanceof ChatInputInteractionEvent) {
            challengerId = ((ChatInputInteractionEvent) event).getOption("player").get().getValue().get().asString();
        } else if (event instanceof MessageInteractionEvent) {
        }

        Optional<ChallengeModel> challenge;

        challenge = getRelevantChallenge(challenges, challengerId);
        if (!canExecute) return;

        service.addNewPlayerIfPlayerNotPresent(guildId, acceptorId);
        challenge.get().setAccepted(true);// TODO Optional?
        queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY, game.getAcceptedChallengeDecayTime(), challenge.get().getMessageId());
        service.saveChallenge(challenge.get());
        addBotReply(String.format("Challenge accepted! Come back and /report when your game is finished."));
    }

    private Optional<ChallengeModel> inferRelevantChallenge(List<ChallengeModel> challenges) {// TODO vllt noch nuetzlich
        challenges.removeIf(ChallengeModel::isAccepted);
        if (challenges.size() == 0) {
            addBotReply("No open challenge present against you");// TODO acyeptierte challenges?
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

    private Optional<ChallengeModel> getRelevantChallenge(List<ChallengeModel> challenges, String challengerId) {
        Optional<ChallengeModel> challenge = challenges.stream().
                filter(chlng -> chlng.getChallengerId().equals(challengerId))
                .findAny();

        if (challenge.isEmpty()) {// TODO evtl weg
            addBotReply("That player has not yet challenged you");
            canExecute = false;
            return challenge;
        }
        if (challenge.get().isAccepted()) {// TODO evtl weg
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
