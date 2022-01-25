package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@UseToStringForLogging
@Document(collection = "challenge")
public class ChallengeModel {

	public enum ReportStatus {
        NOT_YET_REPORTED,
        WIN,
        LOSE,
        DRAW,
        CANCEL
    }

    public enum ReportIntegrity {
        FIRST_TO_REPORT,
		HARMONY,
        CONFLICT
    }

    @Id
    private long id;
    private long guildId;
    private long challengerId;
    private long challengerMessageId;
    private long challengerChannelId;
    private long acceptorId;
    private long acceptorMessageId;
    private long acceptorChannelId;

    private boolean isAccepted = false;
    private ReportStatus challengerReported = ReportStatus.NOT_YET_REPORTED;
    private ReportStatus acceptorReported = ReportStatus.NOT_YET_REPORTED;
    private boolean challengerCalledForCancel = false;
    private boolean acceptorCalledForCancel = false;
    private boolean challengerCalledForRedo = false;
    private boolean acceptorCalledForRedo = false;
    private boolean isDispute = false;

    public ChallengeModel(long guildId,
                          long challengerId, long challengerMessageId, long challengerChannelId,
                          long acceptorId, long acceptorMessageId, long acceptorChannelId) {
        this.id = challengerMessageId;
        this.guildId = guildId;
        this.challengerId = challengerId;
        this.challengerMessageId = challengerMessageId;
        this.challengerChannelId = challengerChannelId;
        this.acceptorId = acceptorId;
        this.acceptorMessageId = acceptorMessageId;
        this.acceptorChannelId = acceptorChannelId;
    }

    public ReportIntegrity setChallengerReported(ReportStatus challengerReported) {
        this.challengerReported = challengerReported;

        if (acceptorReported == ReportStatus.NOT_YET_REPORTED) return ReportIntegrity.FIRST_TO_REPORT;

        if (challengerReported == ReportStatus.WIN && acceptorReported == ReportStatus.LOSE) return ReportIntegrity.HARMONY;
        if (challengerReported == ReportStatus.LOSE && acceptorReported == ReportStatus.WIN) return ReportIntegrity.HARMONY;
        if (challengerReported == ReportStatus.DRAW && acceptorReported == ReportStatus.DRAW) return ReportIntegrity.HARMONY;
        if (challengerReported == ReportStatus.CANCEL && acceptorReported == ReportStatus.CANCEL) return ReportIntegrity.HARMONY;

        isDispute = true;
        return ReportIntegrity.CONFLICT;
    }

    public ReportIntegrity setAcceptorReported(ReportStatus acceptorReported) {
        this.acceptorReported = acceptorReported;

        if (challengerReported == ReportStatus.NOT_YET_REPORTED) return ReportIntegrity.FIRST_TO_REPORT;

        if (acceptorReported == ReportStatus.WIN && challengerReported == ReportStatus.LOSE) return ReportIntegrity.HARMONY;
        if (acceptorReported == ReportStatus.LOSE && challengerReported == ReportStatus.WIN) return ReportIntegrity.HARMONY;
        if (acceptorReported == ReportStatus.DRAW && challengerReported == ReportStatus.DRAW) return ReportIntegrity.HARMONY;
        if (acceptorReported == ReportStatus.CANCEL && challengerReported == ReportStatus.CANCEL) return ReportIntegrity.HARMONY;

        isDispute = true;
        return ReportIntegrity.CONFLICT;
    }

    public void redo() {
        challengerReported = ReportStatus.NOT_YET_REPORTED;
        acceptorReported = ReportStatus.NOT_YET_REPORTED;
        challengerCalledForRedo = false;
        acceptorCalledForRedo = false;
        challengerCalledForCancel = false;
        acceptorCalledForCancel = false;
    }
}
