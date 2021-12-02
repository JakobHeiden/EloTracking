package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
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
    private ChallengeModel challenge;
    private boolean isChallengerReport;

    protected Report(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, ChallengeModel.ReportStatus reportStatus) {
        super(msg, service, bot, queue);
        this.needsRegisteredChannel = true;
        this.needsMention = true;
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

        Optional<ChallengeModel> maybeChallenge = service.findChallenge(challengeId);
        if (maybeChallenge.isEmpty()) {
            addBotReply(String.format("No challenge exists towards that player. Use %schallenge to issue one",
                    defaultCommandPrefix));
            return;
        }

        this.challenge = maybeChallenge.get();
        if (challenge.getAcceptedWhen().isEmpty()) {
            addBotReply("This challenge has not been accepted yet and cannot be reported as a win");
            return;
        }

        this.isChallengerReport = (reportingPlayerId.equals(challenge.getChallengerId()));
        processPossiblyInconsistentReporting();
        if (!canExecute) return;

        processConsistentReporting();

        //if only one player reported, send message and return
        if (challenge.getChallengerReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED ||
                challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED) {
            addBotReply("reported.");
            return;
        }

        //both players have reported consistently, so resolve the challenge into a match
        String winnerId = this.isWin ? reportingPlayerId : reportedOnPlayerId;
        String loserId = this.isWin ? reportedOnPlayerId : reportingPlayerId;
        Match match = new Match(UUID.randomUUID(), channelId, new Date(), winnerId, loserId, false, false);
        double[] resolvedRatings = service.updateRatings(match);
        match.setHasUpdatedPlayerRatings(true);
        service.saveMatch(match);
        service.deleteChallenge(challenge.getId());

        String winnerMention = this.isWin ? msg.getAuthor().get().getMention() : msg.getUserMentionIds().iterator().next().asString();
        String loserMention = this.isWin ? String.format("<@!%s>", msg.getUserMentionIds().iterator().next().asString()) : msg.getAuthor().get().getMention();
        addBotReply(String.format("%s old rating %d, new rating %d. %s old rating %d, new rating %d",
                winnerMention, (int) resolvedRatings[0], (int) resolvedRatings[2],
                loserMention,  (int) resolvedRatings[1], (int) resolvedRatings[3]));
    }

    private void processPossiblyInconsistentReporting() {
        ChallengeModel.ReportStatus reportedOnPlayerReported = this.isChallengerReport ?
                challenge.getAcceptorReported()
                : challenge.getChallengerReported();
        if (this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.WIN) {
            addBotReply("Both reported win");
            service.saveChallenge(challenge);
            this.canExecute = false;
        }
        if (!this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.LOSS) {
            addBotReply("Both reported loss");
            service.saveChallenge(challenge);
            this.canExecute = false;
        }
    }

    private void processConsistentReporting() {
        if (this.isChallengerReport) {
            challenge.setChallengerReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        } else {
            challenge.setAcceptorReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        }
        service.saveChallenge(challenge);
    }
}
