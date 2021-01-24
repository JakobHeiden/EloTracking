package de.neuefische.elotracking.backend.command;

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

import java.util.*;

import static org.mockito.Mockito.*;

public class AcceptTest {
    //arrange
    final DiscordBot bot = mock(DiscordBot.class);
    final EloTrackingService service = mock(EloTrackingService.class);
    final Message msg = mock(Message.class);
    final User recipient = mock(User.class);
    final Channel channel = mock(Channel.class);

    final String recipientId = "1";
    final String challengerId = "2";
    final String channelId = "3";
    final Snowflake authorSnowflake = Snowflake.of(recipientId);
    final Snowflake challengerSnowflake = Snowflake.of(challengerId);
    final Snowflake channelSnowflake = Snowflake.of(channelId);
    final Game game = new Game(channelId, "testgame");
    ChallengeModel challenge = new ChallengeModel(channelId, challengerId, recipientId);
    List<ChallengeModel> challenges = new LinkedList<ChallengeModel>();
    Set<Snowflake> mentionIdsThatIncludeChallenger = Set.of(challengerSnowflake);
    final Command accept = new Accept(bot, service, msg, channel);

    @BeforeEach
    void setupMockBehavior() {
        when(service.findGameByChannelId(channelId)).thenReturn(Optional.of(game));
        when(msg.getAuthor()).thenReturn(Optional.of(recipient));
        when(recipient.getId()).thenReturn(authorSnowflake);
        when(channel.getId()).thenReturn(channelSnowflake);
        when(service.findAllChallengesOfPlayerForChannel(recipientId, channelId)).thenReturn(challenges);
    }

    @AfterEach
    void printBotReplies() {
        for (String botReply : accept.botReplies) {
            System.out.println(botReply);
        }
    }

    @Test
    @DisplayName("Accepting with a mention when no open challenge is present should not result in function calls")
    void mentionButNoChallengePresent() {
        //arrange
        when(msg.getUserMentionIds()).thenReturn(mentionIdsThatIncludeChallenger);

        //act
        accept.execute();

        //assert
        verify(service, never()).addNewPlayerIfPlayerNotPresent(channelId, recipientId);
        verify(service, never()).saveChallenge(any(ChallengeModel.class));
    }

    @Test
    @DisplayName("Accepting with a mention and an open challenge should result in function calls")
    void mentionAndChallengePresent() {
        //arrange
        when(msg.getUserMentionIds()).thenReturn(mentionIdsThatIncludeChallenger);
        challenges.add(challenge);

        //act
        accept.execute();

        //assert
        verify(service).addNewPlayerIfPlayerNotPresent(channelId, recipientId);
        verify(service).saveChallenge(challenge);
    }

    //Tests without a mention in command
    @Test
    @DisplayName("No mention and no challenge should not result in function calls")
    void neitherMentionNorChallengePresent() {
        //arrange
        when(msg.getUserMentionIds()).thenReturn(Set.of());

        //act
        accept.execute();

        //assert
        verify(service, never()).addNewPlayerIfPlayerNotPresent(channelId, recipientId);
        verify(service, never()).saveChallenge(any(ChallengeModel.class));
    }

    @Test
    @DisplayName("No mention and one challenge should result in function calls")
    void noMentionButOneChallengePresent() {
        //arrange
        when(msg.getUserMentionIds()).thenReturn(Set.of());
        challenges.add(challenge);

        //act
        accept.execute();

        //assert
        verify(service).addNewPlayerIfPlayerNotPresent(channelId, recipientId);
        verify(service).saveChallenge(challenge);
    }

    @Test
    @DisplayName("No mention and several challenges should not result in function calls")
    void noMentionButSeveralChallengesPresent() {
        //arrange
        when(msg.getUserMentionIds()).thenReturn(Set.of());
        challenges.add(challenge);
        challenges.add(new ChallengeModel(channelId, "4", recipientId));

        //act
        accept.execute();

        //assert
        verify(service, never()).addNewPlayerIfPlayerNotPresent(channelId, recipientId);
        verify(service, never()).saveChallenge(any(ChallengeModel.class));
    }
}
