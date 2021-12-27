package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
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

    private long guildId;
    private long challengerId;
    @Id
    private long challengerMessageId;
    private long acceptorId;
    private long acceptorMessageId;

    private boolean isAccepted;
    private ReportStatus challengerReported;
    private ReportStatus acceptorReported;
    private boolean challengerCalledForCancel = false;
    private boolean acceptorCalledForCancel = false;
    private boolean challengerCalledForRedo = false;
    private boolean acceptorCalledForRedo = false;

    public ChallengeModel(long guildId,
                          long challengerId, long challengerMessageId,
                          long acceptorId, long acceptorMessageId) {
        this.guildId = guildId;
        this.challengerId = challengerId;
        this.challengerMessageId = challengerMessageId;
        this.acceptorId = acceptorId;
        this.acceptorMessageId = acceptorMessageId;

        this.isAccepted = false;
        this.challengerReported = ReportStatus.NOT_YET_REPORTED;
        this.acceptorReported = ReportStatus.NOT_YET_REPORTED;
    }

    public ReportIntegrity setChallengerReported(ReportStatus challengerReported) {
        this.challengerReported = challengerReported;

        if (acceptorReported == ReportStatus.NOT_YET_REPORTED) return ReportIntegrity.FIRST_TO_REPORT;

        if (challengerReported == ReportStatus.WIN && acceptorReported == ReportStatus.LOSE) return ReportIntegrity.HARMONY;
        if (challengerReported == ReportStatus.LOSE && acceptorReported == ReportStatus.WIN) return ReportIntegrity.HARMONY;
        if (challengerReported == ReportStatus.DRAW && acceptorReported == ReportStatus.DRAW) return ReportIntegrity.HARMONY;
        if (challengerReported == ReportStatus.CANCEL && acceptorReported == ReportStatus.CANCEL) return ReportIntegrity.HARMONY;

        return ReportIntegrity.CONFLICT;
    }

    public ReportIntegrity setAcceptorReported(ReportStatus acceptorReported) {
        this.acceptorReported = acceptorReported;

        if (challengerReported == ReportStatus.NOT_YET_REPORTED) return ReportIntegrity.FIRST_TO_REPORT;

        if (acceptorReported == ReportStatus.WIN && challengerReported == ReportStatus.LOSE) return ReportIntegrity.HARMONY;
        if (acceptorReported == ReportStatus.LOSE && challengerReported == ReportStatus.WIN) return ReportIntegrity.HARMONY;
        if (acceptorReported == ReportStatus.DRAW && challengerReported == ReportStatus.DRAW) return ReportIntegrity.HARMONY;
        if (acceptorReported == ReportStatus.CANCEL && challengerReported == ReportStatus.CANCEL) return ReportIntegrity.HARMONY;

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
