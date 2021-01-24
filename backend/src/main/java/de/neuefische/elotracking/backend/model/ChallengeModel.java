package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private String recipientId;
    private Date issuedWhen;
    private Optional<Date> acceptedWhen;
    private ReportStatus challengerReported;
    private ReportStatus recipientReported;

    public ChallengeModel(String channelId, String challengerId, String recipientId) {
        this.channelId = channelId;
        this.challengerId = challengerId;
        this.recipientId = recipientId;
        this.id = generateId(channelId, challengerId, recipientId);
        this.issuedWhen = new Date();
        this.acceptedWhen = Optional.empty();
        this.challengerReported = ReportStatus.NOT_YET_REPORTED;
        this.recipientReported = ReportStatus.NOT_YET_REPORTED;
    }

    public boolean isAccepted() {
        return !acceptedWhen.isEmpty();
    }

   public static String generateId(String channelId, String playerId1, String playerId2) {
        return playerId1.compareTo(playerId2) < 0 ?
                String.format("%s-%s-%s", channelId, playerId1, playerId2) :
                String.format("%s-%s-%s", channelId, playerId2, playerId1);
    }
}
