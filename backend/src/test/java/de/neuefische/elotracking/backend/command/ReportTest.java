package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.common.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportTest {
    //arrange
    final DiscordBot bot = mock(DiscordBot.class);
    final EloTrackingService service = mock(EloTrackingService.class);
    final Message msg = mock(Message.class);
    final User reportingPlayer = mock(User.class);

    final String reportingPlayerId = "1";
    final String reportedOnPlayerId = "2";
    final String channelId = "3";
    final Snowflake reportingPlayerSnowflake = Snowflake.of(reportingPlayerId);
    final Snowflake reportedOnPlayerSnowflake = Snowflake.of(reportedOnPlayerId);
    final Snowflake channelSnowflake = Snowflake.of(channelId);
    final Game game = new Game(channelId, "testgame");
    final ApplicationPropertiesLoader applicationPropertiesLoader = mock(ApplicationPropertiesLoader.class);
    Command report;
    List<ChallengeModel> challenges;

    @BeforeEach
    void setupMockBehavior() {
        when(service.findGameByChannelId(channelId)).thenReturn(Optional.of(game));
        when(msg.getAuthor()).thenReturn(Optional.of(reportingPlayer));
        when(msg.getChannelId()).thenReturn(Snowflake.of(channelId));
        when(reportingPlayer.getId()).thenReturn(reportingPlayerSnowflake);
        when(service.getConfig()).thenReturn(applicationPropertiesLoader);
        when(applicationPropertiesLoader.getProperty("DEFAULT_COMMAND_PREFIX")).thenReturn("!");
    }

    @AfterEach
    void printBotReplies() {
        for (String botReply : report.botReplies) {
            System.out.println(botReply);
        }
    }

    @Nested
    class withAMention {

        @Nested
        class noAcceptedChallenge {

            @ParameterizedTest
            @EnumSource(names = {"WIN", "LOSS"})
            @DisplayName("Reporting a win or loss with no challenge should not result in function calls")
            void noChallengePresent(ChallengeModel.ReportStatus winOrLoss) {
                //arrange
                when(msg.getUserMentionIds()).thenReturn(Set.of(reportedOnPlayerSnowflake));
                when(service.findChallenge(ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId)))
                        .thenReturn(Optional.empty());
                report = new Report(bot, service, msg, winOrLoss);

                //act
                report.execute();

                //assert
                verify(service, never()).saveChallenge(any());
                verify(service, never()).saveMatch(any());
                verify(service, never()).deleteChallenge(any());
            }

            @ParameterizedTest
            @EnumSource(names = {"WIN", "LOSS"})
            @DisplayName("Reporting a win or loss with an open but not accepted challenge, should not result in function calls")
            void challengePresentButNotYetAccepted(ChallengeModel.ReportStatus winOrLoss) {
                //arrange
                when(msg.getUserMentionIds()).thenReturn(Set.of(reportedOnPlayerSnowflake));
                ChallengeModel challenge = new ChallengeModel(channelId, reportingPlayerId, reportedOnPlayerId);
                when(service.findChallenge(ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId)))
                        .thenReturn(Optional.of(challenge));
                report = new Report(bot, service, msg, winOrLoss);

                //act
                report.execute();

                //assert
                verify(service, never()).saveChallenge(challenge);
                verify(service, never()).saveMatch(any());
                verify(service, never()).deleteChallenge(any());
            }
        }

        @Nested
        class acceptedChallengePresent {

            @ParameterizedTest
            @EnumSource(names = {"WIN", "LOSS"})
            @DisplayName("Reporting a win or loss with the other player not having reported yet, should result in function calls")
            void acceptedChallengePresent(ChallengeModel.ReportStatus winOrLoss) {
                //arrange
                when(msg.getUserMentionIds()).thenReturn(Set.of(reportedOnPlayerSnowflake));
                ChallengeModel challenge = new ChallengeModel(channelId, reportingPlayerId, reportedOnPlayerId);
                challenge.setAcceptedWhen(Optional.of(new Date()));
                when(service.findChallenge(ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId)))
                        .thenReturn(Optional.of(challenge));
                report = new Report(bot, service, msg, winOrLoss);

                //act
                report.execute();

                //assert
                verify(service).saveChallenge(challenge);
                verify(service, never()).saveMatch(any());
                verify(service, never()).deleteChallenge(any());
            }

            @ParameterizedTest
            @EnumSource(names = {"WIN", "LOSS"})
            @DisplayName("Reporting a win or loss with the other player having reported correctly, should result in function calls")
            void acceptedChallengePresentAndOtherPlayerReportedCorrectly(ChallengeModel.ReportStatus winOrLoss) {
                //arrange
                when(msg.getUserMentionIds()).thenReturn(Set.of(reportedOnPlayerSnowflake));
                ChallengeModel challenge = new ChallengeModel(channelId, reportingPlayerId, reportedOnPlayerId);
                challenge.setAcceptedWhen(Optional.of(new Date()));
                when(service.findChallenge(ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId)))
                        .thenReturn(Optional.of(challenge));
                Message otherPlayerMessage = mock(Message.class);
                when(otherPlayerMessage.getUserMentionIds()).thenReturn(Set.of(reportingPlayerSnowflake));
                when(otherPlayerMessage.getChannelId()).thenReturn(Snowflake.of(channelId));
                final User reportedOnPlayer = mock(User.class);
                when(otherPlayerMessage.getAuthor()).thenReturn(Optional.of(reportedOnPlayer));
                when(reportedOnPlayer.getId()).thenReturn(reportedOnPlayerSnowflake);
                Report otherPlayerReport;
                if (winOrLoss == ChallengeModel.ReportStatus.LOSS) {
                    otherPlayerReport = new Report(bot, service, otherPlayerMessage, ChallengeModel.ReportStatus.WIN);
                } else {
                    otherPlayerReport = new Report(bot, service, otherPlayerMessage, ChallengeModel.ReportStatus.LOSS);
                }
                when(service.updateRatings(any())).thenReturn(new double[]{1, 2, 3, 4});
                report = new Report(bot, service, msg, winOrLoss);

                //act
                otherPlayerReport.execute();
                report.execute();

                //assert
                verify(service, times(2)).saveChallenge(challenge);
                verify(service).saveMatch(any());
                verify(service).deleteChallenge(challenge);
                verify(service).updateRatings(any());
            }

            @ParameterizedTest
            @EnumSource(names = {"WIN", "LOSS"})
            @DisplayName("Reporting a win or loss with the other player having reported a conflicting result should not result in function calls")
            void acceptedChallengePresentAndOtherPlayerReportedIncorrectly(ChallengeModel.ReportStatus winOrLoss) {
                //arrange
                when(msg.getUserMentionIds()).thenReturn(Set.of(reportedOnPlayerSnowflake));
                ChallengeModel challenge = new ChallengeModel(channelId, reportingPlayerId, reportedOnPlayerId);
                challenge.setAcceptedWhen(Optional.of(new Date()));
                when(service.findChallenge(ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId)))
                        .thenReturn(Optional.of(challenge));
                challenge.setRecipientReported(winOrLoss);
                report = new Report(bot, service, msg, winOrLoss);

                //act
                report.execute();

                //assert
                verify(service).saveChallenge(challenge);
                verify(service, never()).saveMatch(any());
                verify(service, never()).deleteChallenge(any());
            }
        }
    }
}
