package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.*;
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
public class CancelTest {

    @Mock
    private EloTrackingService service;
    @Mock
    private DiscordBotService bot;
    private Message msg;
    private Command cancel;
    private List<ChallengeModel> challenges = ChallengeModelTestFactory.createList();

    @BeforeEach
    void initService() {
        when(service.findGameByChannelId(CHANNEL_ID)).thenReturn(GameTestFactory.create());
        //when(service.findAllChallengesOfAcceptorForChannel(ACCEPTOR_ID, CHANNEL_ID)).thenReturn(challenges);
    }

    @AfterEach
    void printBotReplies() {
        cancel.getBotReplies().forEach(System.out::println);
    }

    @ParameterizedTest
    @ValueSource(strings = {"!cancel @%s", "!cancel"})
    @DisplayName("No challenge should not call service")
    void noChallengeOrMatch(String text) {
        text = String.format(text, CHALLENGER_ID);
        msg = MessageTestFactory.createMock(text, CHALLENGER);
        int i = 1;
        cancel = new Cancel(msg, service, bot);

        cancel.execute();

        verify(service, never()).deleteChallenge(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {CHALLENGER_ID, ACCEPTOR_ID})
    @DisplayName("No mention and more than one challenge should not call service")
    void noMentionMoreThanOneChallengeOrMatch(String whoDoesTheCancel) {
        challenges.add(ChallengeModelTestFactory.create());
        challenges.add(new ChallengeModel(CHANNEL_ID, CHALLENGER_ID, SnowflakeTestFactory.createId()));
        challenges.add(new ChallengeModel(CHANNEL_ID, SnowflakeTestFactory.createId(), ACCEPTOR_ID));
        msg = MessageTestFactory.createMock("!cancel", Snowflake.of(whoDoesTheCancel));
        cancel = new Cancel(msg, service, bot);

        cancel.execute();

        verify(service, never()).deleteChallenge(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {CHALLENGER_ID, ACCEPTOR_ID})
    @DisplayName("No mention and one challenge should call service")
    void noMentionOneChallengeOrMatch(String whoDoesTheCancel) {
        challenges.add(ChallengeModelTestFactory.create());
        msg = MessageTestFactory.createMock("!cancel", Snowflake.of(whoDoesTheCancel));
        cancel = new Cancel(msg, service, bot);

        cancel.execute();

        verify(service).deleteChallenge(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("One mention and a matching challenge should call service")
    void oneMentionAndMatchingChallengeOrMatch(boolean switchIdsAround) {
        challenges.add(ChallengeModelTestFactory.create());
        msg = MessageTestFactory.createMock(String.format("!cancel @%s",
                switchIdsAround ? ACCEPTOR_ID : CHALLENGER_ID),
                Snowflake.of(switchIdsAround ? CHALLENGER_ID : ACCEPTOR_ID));
        cancel = new Cancel(msg, service, bot);

        cancel.execute();

        verify(service).deleteChallenge(any());
    }

    @Test
    @DisplayName("One mention and no matching challenge should not call service")
    void oneMentionNoMatchingChallengeOrMatch() {
        challenges.add(new ChallengeModel(CHANNEL_ID, CHALLENGER_ID, SnowflakeTestFactory.createId()));
        msg = MessageTestFactory.createMock(String.format("!cancel @%s", ACCEPTOR_ID), CHALLENGER);
        cancel = new Cancel(msg, service, bot);

        cancel.execute();

        verify(service, never()).deleteChallenge(any());
    }
}
