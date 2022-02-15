package com.elorankingbot.backend.model;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

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

    private UUID id;
    private UUID gameId;
    private long challengerUserId;
    private long challengerMessageId;
    private long challengerChannelId;
    private String challengerTag;
    private long acceptorUserId;
    private long acceptorMessageId;
    private long acceptorChannelId;
    private String acceptorTag;

    private boolean isAccepted = false;
    private ReportStatus challengerReported = ReportStatus.NOT_YET_REPORTED;
    private ReportStatus acceptorReported = ReportStatus.NOT_YET_REPORTED;
    private boolean challengerCalledForCancel = false;
    private boolean acceptorCalledForCancel = false;
    private boolean challengerCalledForRedo = false;
    private boolean acceptorCalledForRedo = false;
    private boolean isDispute = false;

    public ChallengeModel(long guildId,
                          long challengerUserId, long challengerMessageId, long challengerChannelId, String challengerTag,
                          long acceptorUserId, long acceptorMessageId, long acceptorChannelId, String acceptorTag) {
        this.id = UUID.randomUUID();
        this.challengerUserId = challengerUserId;
        this.challengerMessageId = challengerMessageId;
        this.challengerChannelId = challengerChannelId;
        this.challengerTag = challengerTag;
        this.acceptorUserId = acceptorUserId;
        this.acceptorMessageId = acceptorMessageId;
        this.acceptorChannelId = acceptorChannelId;
        this.acceptorTag = acceptorTag;
    }

    public ReportIntegrity setChallengerReported(ReportStatus challengerReported) {
        this.challengerReported = challengerReported;

        if (acceptorReported == ReportStatus.NOT_YET_REPORTED) return ReportIntegrity.FIRST_TO_REPORT;// TODO duplizieren wegmachen?

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

    public boolean hasAReport() {
        return (acceptorReported != ReportStatus.NOT_YET_REPORTED) || (challengerReported != ReportStatus.NOT_YET_REPORTED);
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
