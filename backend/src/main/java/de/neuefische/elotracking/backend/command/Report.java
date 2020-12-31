package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class Report extends Command {
    private final boolean isWin;

    public Report(DiscordBot bot, EloTrackingService service, Message msg, Channel channel, ChallengeModel.ReportStatus reportStatus) {
        super(bot, service, msg, channel);
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

        String channelId = channel.getId().asString();
        String reportingPlayerId = msg.getAuthor().get().getId().asString();
        String reportedOnPlayerId = msg.getUserMentionIds().iterator().next().asString();
        String challengeId = ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId);
        Optional<ChallengeModel> challenge = service.findChallenge(challengeId);

        if (challenge.isEmpty()) {
            canExecute = false;
            botReplies.add(String.format("No challenge exists towards that player. Use %schallenge to issue one",
                    service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX")));
        }
        if (challenge.isPresent()) {
            if (challenge.get().getAcceptedWhen().isEmpty()) {
                canExecute = false;
                botReplies.add("This challenge has not been accepted yet and cannot be reported as a win");
            }
        }

        //check if there is inconsistent reporting
        boolean isChallengerReport = (reportingPlayerId.equals(challenge.get().getChallengerId()));
        ChallengeModel.ReportStatus reportedOnPlayerReported = isChallengerReport ?
                challenge.get().getRecipientReported()
                : challenge.get().getChallengerReported();
        if (this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.WIN) {
            canExecute = false;
            botReplies.add("Both reported win");
        }
        if (!this.isWin && reportedOnPlayerReported == ChallengeModel.ReportStatus.LOSS) {
            canExecute = false;
            botReplies.add("Both reported loss");
        }
        if (!canExecute) return;

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
