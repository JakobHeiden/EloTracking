package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import discord4j.core.object.entity.Message;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public abstract class Report extends Command {

    private final boolean isWin;

    protected Report(Message msg, ChallengeModel.ReportStatus reportStatus) {
        super(msg);
        this.needsRegisteredChannel = true;
        this.needsUserTag = true;
        this.isWin = (reportStatus == ChallengeModel.ReportStatus.WIN);
    }

    public static String getDescription() {
        return "!win [@player] - Report a win over another player";
    }
    public static String getDescription2() {
        return "!lose [@player] - Report a loss to another player";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        if (!canExecute) return;

        String reportingPlayerId = msg.getAuthor().get().getId().asString();
        String reportedOnPlayerId = msg.getUserMentionIds().iterator().next().asString();
        String challengeId = ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId);
        Optional<ChallengeModel> challenge = service.findChallenge(challengeId);

        if (challenge.isEmpty()) {
            botReplies.add(String.format("No challenge exists towards that player. Use %schallenge to issue one",
                    service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX")));
            return;
        } else {
            if (challenge.get().getAcceptedWhen().isEmpty()) {
                botReplies.add("This challenge has not been accepted yet and cannot be reported as a win");
                return;
            }
        }

        //check if there is inconsistent reporting
        boolean isChallengerReport = (reportingPlayerId.equals(challenge.get().getChallengerId()));
        ChallengeModel.ReportStatus reportedOnPlayerReported = isChallengerReport ?
                challenge.get().getRecipientReported()
                : challenge.get().getChallengerReported();
        if (this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.WIN) {
            botReplies.add("Both reported win");
            service.saveChallenge(challenge.get());
            return;
        }
        if (!this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.LOSS) {
            botReplies.add("Both reported loss");
            service.saveChallenge(challenge.get());
            return;
        }

        //set report status
        if (isChallengerReport) {
            challenge.get().setChallengerReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        } else {
            challenge.get().setRecipientReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        }
        service.saveChallenge(challenge.get());

        //if only one player reported, send message and return
        if (challenge.get().getChallengerReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED ||
                challenge.get().getRecipientReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED) {
            botReplies.add("reported.");
            return;
        }

        //both players have reported consistently, so resolve the challenge into a match
        String winnerId = this.isWin ? reportingPlayerId : reportedOnPlayerId;
        String loserId = this.isWin ? reportedOnPlayerId : reportingPlayerId;
        Match match = new Match(UUID.randomUUID(), channelId, new Date(), winnerId, loserId, false, false);
        double[] resolvedRatings = service.updateRatings(match);
        match.setHasUpdatedPlayerRatings(true);
        service.saveMatch(match);
        service.deleteChallenge(challenge.get());

        String winnerMention = this.isWin ? msg.getAuthor().get().getMention() : msg.getUserMentionIds().iterator().next().asString();
        String loserMention = this.isWin ? String.format("<@!%s>", msg.getUserMentionIds().iterator().next().asString()) : msg.getAuthor().get().getMention();
        botReplies.add(String.format("%s old rating %d, new rating %d. %s old rating %d, new rating %d",
                winnerMention, (int) resolvedRatings[0], (int) resolvedRatings[2],
                loserMention,  (int) resolvedRatings[1], (int) resolvedRatings[3]));
    }
}
