package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.Message;

import java.util.Optional;

public abstract class Report extends Command {

    private final boolean isWin;
    private boolean canExecute;
    private String reportingPlayerId;
    private String reportedOnPlayerId;
    private String challengeId;
    private ChallengeModel challenge;
    private boolean isChallengerReport;

    protected Report(Event msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, ChallengeModel.ReportStatus reportStatus) {
        super(msg, service, bot, queue);
        this.isWin = (reportStatus == ChallengeModel.ReportStatus.WIN);
    }
/*
    public void execute() {
        this.canExecute = super.checkForGame();
        if (!canExecute) return;

        this.reportingPlayerId = msg.getAuthor().get().getId().asString();
        this.reportedOnPlayerId = msg.getUserMentionIds().iterator().next().asString();
        //this.challengeId = ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId);

        Optional<ChallengeModel> maybeChallenge = service.findChallenge(challengeId);
        if (maybeChallenge.isEmpty()) {
            addBotReply(String.format("No challenge exists towards that player. Use %schallenge to issue one",
                    defaultCommandPrefix));
            return;
        }

        this.challenge = maybeChallenge.get();
        if (!challenge.isAccepted()) {
            addBotReply("This challenge has not been accepted yet and cannot be reported as a win");
            return;
        }

        this.isChallengerReport = (reportingPlayerId.equals(challenge.getChallengerId()));
        processPossiblyInconsistentReporting();
        if (!canExecute) return;

        processOneSidedOrConsistentReporting();

        //if only one player reported, send message and return
        if (challenge.getChallengerReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED ||
                challenge.getAcceptorReported() == ChallengeModel.ReportStatus.NOT_YET_REPORTED) {
            //queue.addTimedTask(TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE, game.getMatchAutoResolveTime(), challengeId);
            addBotReply("reported.");
            return;
            // TODO! Setter commands f[r auto resolve und dings
        }

        //both players have reported consistently, so resolve the challenge into a match
        String winnerId = this.isWin ? reportingPlayerId : reportedOnPlayerId;
        String loserId = this.isWin ? reportedOnPlayerId : reportingPlayerId;
        Match match = new Match(channelId, winnerId, loserId, false);
        double[] resolvedRatings = service.updateRatings(match);
        service.saveMatch(match);
        service.deleteChallenge(challenge.getId());

        addBotReply(String.format("<@%s> old rating %d, new rating %d. <@%s> old rating %d, new rating %d",
                winnerId, (int) resolvedRatings[0], (int) resolvedRatings[2],
                loserId,  (int) resolvedRatings[1], (int) resolvedRatings[3]));
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

    private void processOneSidedOrConsistentReporting() {
        if (this.isChallengerReport) {
            challenge.setChallengerReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        } else {
            challenge.setAcceptorReported(this.isWin ? ChallengeModel.ReportStatus.WIN : ChallengeModel.ReportStatus.LOSS);
        }
        service.saveChallenge(challenge);
    }*/
}
