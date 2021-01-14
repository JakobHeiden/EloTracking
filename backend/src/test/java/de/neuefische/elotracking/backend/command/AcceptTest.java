package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
public class AcceptTest {
    //arrange
    final DiscordBot bot = mock(DiscordBot.class);
    final EloTrackingService service = mock(EloTrackingService.class);
    final Message msg = mock(Message.class);
    final User author = mock(User.class);
    final Snowflake authorSnowflake = mock(Snowflake.class);
    final Channel channel = mock(Channel.class);
    final Snowflake channelSnowflake = mock(Snowflake.class);

    final String authorId = "1";
    final String channelId = "2";
    final String challengerId = "3";
    final Game game = new Game(channelId, "testgame");
    final Command accept = new Accept(bot, service, msg, channel);

    @BeforeEach
    void setupMockup() {
        when(service.findGameByChannelId(channelId)).thenReturn(Optional.of(game));
        when(msg.getAuthor()).thenReturn(Optional.of(author));
        when(author.getId()).thenReturn(authorSnowflake);
        when(authorSnowflake.asString()).thenReturn(authorId);
        when(channel.getId()).thenReturn(channelSnowflake);
        when(channelSnowflake.asString()).thenReturn(channelId);
    }

    @Test
    @DisplayName("Accepting when no open challenge is present should not lead to function calls")
    void noOpenChallengePresent() {
        //arrange
        when(service.findChallengesOfPlayerForChannel(authorId, channelId))
                .thenReturn(new LinkedList<ChallengeModel>());

        //act
        accept.execute();

        //assert
        verify(service, never()).addNewPlayerIfPlayerNotPresent(channelId, authorId);
        verify(service, never()).saveChallenge(any(ChallengeModel.class));
    }

    @Test
    @DisplayName("Accepting an open challenge should lead to function calls")
    void openChallengePresent() {
        //arrange
        List listOfChallenges = new LinkedList<ChallengeModel>();
        ChallengeModel challenge = new ChallengeModel(channelId, challengerId, authorId);
        listOfChallenges.add(challenge);
        when(service.findChallengesOfPlayerForChannel(authorId, channelId))
                .thenReturn(listOfChallenges);

        //act
        accept.execute();

        //assert
        verify(service).addNewPlayerIfPlayerNotPresent(channelId, authorId);
        verify(service).saveChallenge(challenge);
    }


}
