package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "challenge")
public class Challenge {
    public enum ReportStatus {
        NOT_YET_REPORTED,
        WIN,
        LOSS
    }

    @Id
    private String id;
    private String channelId;
    private String challengerId;
    private String otherPlayerId;
    private Date issuedWhen;
    private Optional<Date> acceptedWhen;
    private ReportStatus challengerReported;
    private ReportStatus otherPlayerReported;

    public Challenge(String channelId, String challengerId, String otherPlayerId) {
        this.channelId = channelId;
        this.challengerId = challengerId;
        this.otherPlayerId = otherPlayerId;
        this.id = generateId(channelId, challengerId, otherPlayerId);
        this.issuedWhen = new Date();
        this.acceptedWhen = Optional.empty();
        this.challengerReported = ReportStatus.NOT_YET_REPORTED;
        this.otherPlayerReported = ReportStatus.NOT_YET_REPORTED;
    }

    public void accept() {
        this.acceptedWhen = Optional.of(new Date());
    }

    public void report(boolean isChallengerReport, boolean isWin) {
        if (isChallengerReport) {
            challengerReported = isWin ? ReportStatus.WIN : ReportStatus.LOSS;
        } else {
            otherPlayerReported = isWin ? ReportStatus.WIN : ReportStatus.LOSS;
        }
    }

    public static String generateId(String channelId, String playerId1, String playerId2) {
        return playerId1.compareTo(playerId2) < 0 ?
                String.format("%s-%s-%s", channelId, playerId1, playerId2) :
                String.format("%s-%s-%s", channelId, playerId2, playerId1);
    }
}
