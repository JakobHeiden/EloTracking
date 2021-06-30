package de.neuefische.elotracking.backend.model;

import de.neuefische.elotracking.backend.aop.UseToStringForLogging;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Optional;

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
    private String id;
    private String channelId;
    private String challengerId;
    private String acceptorId;
    private Date issuedWhen;
    private Optional<Date> acceptedWhen;
    private ReportStatus challengerReported;
    private ReportStatus acceptorReported;
    private boolean challengerCalledForCancel = false;
    private boolean acceptorCalledForCancel = false;

    public ChallengeModel(String channelId, String challengerId, String acceptorId) {
        this.id = generateId(channelId, challengerId, acceptorId);
        this.channelId = channelId;
        this.challengerId = challengerId;
        this.acceptorId = acceptorId;
        this.issuedWhen = new Date();
        this.acceptedWhen = Optional.empty();
        this.challengerReported = ReportStatus.NOT_YET_REPORTED;
        this.acceptorReported = ReportStatus.NOT_YET_REPORTED;
    }

    public boolean isAccepted() {
        return !acceptedWhen.isEmpty();
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

    public static String generateId(String channelId, String challengerId, String acceptorId) {
        return challengerId.compareTo(acceptorId) < 0 ?
                String.format("%s-%s-%s", channelId, challengerId, acceptorId) :
                String.format("%s-%s-%s", channelId, acceptorId, challengerId);
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
        this.id = generateId(channelId, this.challengerId, this.acceptorId);
    }
    
    public void setChallengerId(String challengerId) {
        this.challengerId = challengerId;
        this.id = generateId(channelId, this.challengerId, this.acceptorId);
    }

    public void setAcceptorId(String acceptorId) {
        this.acceptorId = acceptorId;
        this.id = generateId(channelId, this.challengerId, this.acceptorId);
    }
}
