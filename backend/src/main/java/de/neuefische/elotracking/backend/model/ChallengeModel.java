package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.logging.UseToStringForLogging;
import lombok.*;
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
        LOSS
    }

    @Id
    private long messageId;
    @Getter
    private long guildId;
    private long challengerId;
    private long acceptorId;
    private boolean isAccepted;
    private ReportStatus challengerReported;
    private ReportStatus acceptorReported;
    private boolean challengerCalledForCancel = false;
    private boolean acceptorCalledForCancel = false;

    public ChallengeModel(long guildId, long messageId, long challengerId, long acceptorId) {
        this.messageId = messageId;
        this.guildId = guildId;
        this.messageId = messageId;
        this.challengerId = challengerId;
        this.acceptorId = acceptorId;
        this.isAccepted = false;
        this.challengerReported = ReportStatus.NOT_YET_REPORTED;
        this.acceptorReported = ReportStatus.NOT_YET_REPORTED;
    }

    public void callForCancel(String playerId) {
        if (playerId.equals(challengerId)) {
            challengerCalledForCancel = true;
        } else {
            acceptorCalledForCancel = true;
        }
    }

    public boolean shouldBeDeleted() {
        if (!isAccepted()) {
            return challengerCalledForCancel || acceptorCalledForCancel;
        } else {
            return challengerCalledForCancel && acceptorCalledForCancel;
        }
    }
}
