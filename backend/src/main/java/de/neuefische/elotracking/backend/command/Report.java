package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import discord4j.core.object.entity.Message;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public abstract class Report extends Command {

    private final boolean isWin;
    private boolean canExecute;
    private String reportingPlayerId;
    private String reportedOnPlayerId;
    private String challengeId;
    private Optional<ChallengeModel> challenge;
    private boolean isChallengerReport;

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
        this.canExecute = super.canExecute();
        if (!canExecute) return;

        this.reportingPlayerId = msg.getAuthor().get().getId().asString();
        this.reportedOnPlayerId = msg.getUserMentionIds().iterator().next().asString();
        this.challengeId = ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId);
        this.challenge = service.findChallenge(challengeId);

        if (challenge.isEmpty()) {
            botReplies.add(String.format("No challenge exists towards that player. Use %schallenge to issue one",
                    service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX")));
            return;
        }
        if (challenge.get().getAcceptedWhen().isEmpty()) {
            botReplies.add("This challenge has not been accepted yet and cannot be reported as a win");
            return;
        }

        this.isChallengerReport = (reportingPlayerId.equals(challenge.get().getChallengerId()));
        checkForInconsistentReporting();
        if (!canExecute) return;

        setReportStatus();

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

    private void checkForInconsistentReporting() {
        ChallengeModel.ReportStatus reportedOnPlayerReported = this.isChallengerReport ?
                challenge.get().getRecipientReported()
                : challenge.get().getChallengerReported();
        if (this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.WIN) {
            botReplies.add("Both reported win");
            service.saveChallenge(challenge.get());
            this.canExecute = false;
        }
        if (!this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.LOSS) {
            botReplies.add("Both reported loss");
            service.saveChallenge(challenge.get());
            this.canExecute = false;
        }
    }

    private void setReportStatus() {
        if (this.isChallengerReport) {
            challenge.get().setChallengerReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        } else {
            challenge.get().setRecipientReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        }
        service.saveChallenge(challenge.get());
    }
}
