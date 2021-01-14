package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.LinkedList;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AcceptTest {
    //arrange
    @Mock DiscordBot bot;
    final EloTrackingService service = mock(EloTrackingService.class);
    @Mock Message msg;// = mock(Message.class);
    final User author = mock(User.class);
    final Snowflake authorSnowflake = mock(Snowflake.class);
    final Channel channel = mock(Channel.class);
    final Snowflake channelSnowflake = mock(Snowflake.class);
    final Command accept = new Accept(bot, service, msg, channel);

    final String authorId = "1";
    final String channelId = "2";

    @BeforeEach
    void setupMockup() {
        when(msg.getAuthor()).thenReturn(Optional.of(author));
        //when(author.getId()).thenReturn(authorSnowflake);
        //when(authorSnowflake.asString()).thenReturn(authorId);
        when(channel.getId()).thenReturn(channelSnowflake);
        //when(channelSnowflake.asString()).thenReturn(channelId);
    }

    @Test
    @DisplayName("Accepting when no open challenge is present should not lead to function calls")
    void noOpenChallenge() {
        when(service.findChallengesOfPlayerForChannel(authorId, channelId))
                .thenReturn(new LinkedList<ChallengeModel>());

        //act
        accept.execute();

        //assert
        verify(service, never()).addNewPlayerIfPlayerNotPresent(anyString(), anyString());
        verify(service, never()).saveChallenge(any(ChallengeModel.class));
    }


}
