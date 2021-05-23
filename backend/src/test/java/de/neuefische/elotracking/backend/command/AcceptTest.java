package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static de.neuefische.elotracking.backend.command.SnowflakeTestFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptTest {

    @Mock private EloTrackingService service;
    @Mock private DiscordBotService bot;
    private Message msg;
    private Command accept;

    @AfterEach
    void printBotReplies() {
        accept.getBotReplies().forEach(System.out::println);
    }

    @ParameterizedTest
    @ValueSource(strings = {"!accept @%s", "!accept"})
    @DisplayName("No open challenge should not call service")
    void noOpenChallenge(String text) {
        text = String.format(text, CHALLENGERID);
        when(service.findGameByChannelId(CHANNELID)).thenReturn(GameTestFactory.create());
        when(service.findAllChallengesOfRecipientForChannel(RECIPIENTID, CHANNELID))
            .thenReturn(ChallengeModelTestFactory.createList());
        msg = MessageTestFactory.createMock(text, RECIPIENT);
        accept = new Accept(msg, service, bot);

        accept.execute();

        verify(service, never()).addNewPlayerIfPlayerNotPresent(any(), any());
    }

    @Test
    @DisplayName("Mention present but open challenge from a different player should not call service")
    void openChallengeFromDifferentPlayer() {
        String text = String.format("!accept @%s", CHALLENGERID);
        ChallengeModel challenge = ChallengeModelTestFactory.create();
        challenge.setChallengerId(SnowflakeTestFactory.createId());
        List<ChallengeModel> challenges = ChallengeModelTestFactory.createList(challenge);
        when(service.findGameByChannelId(CHANNELID)).thenReturn(GameTestFactory.create());
        when(service.findAllChallengesOfRecipientForChannel(RECIPIENTID, CHANNELID))
                .thenReturn(challenges);
        msg = MessageTestFactory.createMock(text, RECIPIENT);
        accept = new Accept(msg, service, bot);

        accept.execute();

        verify(service, never()).addNewPlayerIfPlayerNotPresent(any(), any());
    }

    @Test
    @DisplayName("No mention but two open challenges should not call service")
    void noMentionTwoChallenges() {}

    @Test
    @DisplayName("One open challenge and no mention should call service")
    void oneChallengeNoMention() {}

    @Test
    @DisplayName("Open challenges and a matching mention should call service")
    void openChallengesMatchingMention() {}
}
