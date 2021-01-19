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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.ParameterizedType;
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
    final Channel channel = mock(Channel.class);

    final String reportingPlayerId = "1";
    final String reportedOnPlayerId = "2";
    final String channelId = "3";
    final Snowflake reportingPlayerSnowflake = Snowflake.of(reportedOnPlayerId);
    final Snowflake reportedOnPlayerSnowflake = Snowflake.of(reportingPlayerId);
    final Snowflake channelSnowflake = Snowflake.of(channelId);
    final Game game = new Game(channelId, "testgame");
    final ApplicationPropertiesLoader applicationPropertiesLoader = mock(ApplicationPropertiesLoader.class);
    Command report;
    List<ChallengeModel> challenges;

    @BeforeEach
    void setupMockBehavior() {
        when(service.findGameByChannelId(channelId)).thenReturn(Optional.of(game));
        when(msg.getAuthor()).thenReturn(Optional.of(reportingPlayer));
        when(reportingPlayer.getId()).thenReturn(reportingPlayerSnowflake);
        when(channel.getId()).thenReturn(channelSnowflake);
        //when(service.findAllChallengesOfPlayerForChannel(reportingPlayerId, channelId)).thenReturn(challenges);
        when(service.getConfig()).thenReturn(applicationPropertiesLoader);
        when(applicationPropertiesLoader.getProperty("DEFAULT_COMMAND_PREFIX")).thenReturn("!");
    }

    @AfterEach
    void printBotReplies() {
        for (String botReply : report.botReplies) {
            System.out.println(botReply);
        }
    }

    @ParameterizedTest
    @EnumSource(names = {"WIN", "LOSS"})
    @DisplayName("Reporting a win or loss with a mention and no open challenge should not result in function calls")
    void mentionButNoMatchPresent(ChallengeModel.ReportStatus winOrLoss) {
        //arrange
        when(msg.getUserMentionIds()).thenReturn(Set.of(reportedOnPlayerSnowflake));
        when(service.findChallenge(ChallengeModel.generateId(channelId, reportingPlayerId, reportedOnPlayerId)))
                .thenReturn(Optional.empty());
        report = new Report(bot, service, msg, channel, winOrLoss);

        //act
        report.execute();

        //assert
        verify(service, never()).saveChallenge(any());
        verify(service, never()).saveMatch(any());
        verify(service, never()).deleteChallenge(any());
    }


}
