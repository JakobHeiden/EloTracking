package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static de.neuefische.elotracking.backend.commands.SnowflakeTestFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CancelTest extends CommandTest {

   private List<ChallengeModel> challenges = ChallengeModelTestFactory.createList();

    @BeforeEach
    void initService() {
        when(service.findGameByChannelId(CHANNEL_ID)).thenReturn(GameTestFactory.create());
        lenient().when(service.findAllChallengesForPlayerForChannel(ACCEPTOR_ID, CHANNEL_ID)).thenReturn(challenges);
        lenient().when(service.findAllChallengesForPlayerForChannel(CHALLENGER_ID, CHANNEL_ID)).thenReturn(challenges);
    }

    @ParameterizedTest
    @ValueSource(strings = {"!cancel @%s", "!cancel"})
    @DisplayName("No challenge should not call service")
    void noChallengeOrMatch(String text) {
        text = String.format(text, CHALLENGER_ID);
        Message msg = MessageTestFactory.createMock(text, CHALLENGER);
        int i = 1;
        command = new Cancel(msg, service, bot);

        command.execute();

        verify(service, never()).deleteChallenge(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {CHALLENGER_ID, ACCEPTOR_ID})
    @DisplayName("No mention and more than one challenge should not call service")
    void noMentionMoreThanOneChallengeOrMatch(String whoDoesTheCancel) {
        challenges.add(ChallengeModelTestFactory.create());
        challenges.add(new ChallengeModel(CHANNEL_ID, CHALLENGER_ID, SnowflakeTestFactory.createId()));
        challenges.add(new ChallengeModel(CHANNEL_ID, SnowflakeTestFactory.createId(), ACCEPTOR_ID));
        Message msg = MessageTestFactory.createMock("!cancel", Snowflake.of(whoDoesTheCancel));
        command = new Cancel(msg, service, bot);

        command.execute();

        verify(service, never()).deleteChallenge(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {CHALLENGER_ID, ACCEPTOR_ID})
    @DisplayName("No mention and one challenge should call service")
    void noMentionOneChallengeOrMatch(String whoDoesTheCancel) {
        challenges.add(ChallengeModelTestFactory.create());
        Message msg = MessageTestFactory.createMock("!cancel", Snowflake.of(whoDoesTheCancel));
        command = new Cancel(msg, service, bot);

        command.execute();

        verify(service).deleteChallenge(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("One mention and a matching challenge should call service")
    void oneMentionAndMatchingChallengeOrMatch(boolean switchIdsAround) {
        challenges.add(ChallengeModelTestFactory.create());
        Message msg = MessageTestFactory.createMock(
                String.format("!cancel @%s", switchIdsAround ? ACCEPTOR_ID : CHALLENGER_ID),
                Snowflake.of(switchIdsAround ? CHALLENGER_ID : ACCEPTOR_ID));
        command = new Cancel(msg, service, bot);

        command.execute();

        verify(service).deleteChallenge(any());
    }

    @Test
    @DisplayName("One mention and no matching challenge should not call service")
    void oneMentionNoMatchingChallengeOrMatch() {
        challenges.add(new ChallengeModel(CHANNEL_ID, CHALLENGER_ID, SnowflakeTestFactory.createId()));
        Message msg = MessageTestFactory.createMock(String.format("!cancel @%s", ACCEPTOR_ID), CHALLENGER);
        command = new Cancel(msg, service, bot);

        command.execute();

        verify(service, never()).deleteChallenge(any());
    }

    @Test
    @DisplayName("An accepted challenge cancelled by one party should not call service")
    void acceptedChallengeOneCancel() {
        ChallengeModel challengeSpy = Mockito.spy(ChallengeModelTestFactory.create());
        challengeSpy.accept();
        challenges.add(challengeSpy);
        Message msg = MessageTestFactory.createMock(String.format("!cancel @%s", ACCEPTOR_ID), CHALLENGER);
        command = new Cancel(msg, service, bot);

        command.execute();

        verify(challengeSpy).callForCancel(CHALLENGER_ID);
        verify(service, never()).deleteChallenge(any());
    }

    @Test
    @DisplayName("An accepted challenge with both parties calling for cancel should call service")
    void acceptedChallengeBothCancel() {
        ChallengeModel challengeSpy = Mockito.spy(ChallengeModelTestFactory.create());
        challengeSpy.accept();
        challenges.add(challengeSpy);
        Message msg = MessageTestFactory.createMock(String.format("!cancel @%s", ACCEPTOR_ID), CHALLENGER);
        command = new Cancel(msg, service, bot);
        Message msg2 = MessageTestFactory.createMock(String.format("!cancel @%s", CHALLENGER_ID), ACCEPTOR);
        Command cancel2 = new Cancel(msg2, service, bot);

        command.execute();
        cancel2.execute();

        verify(challengeSpy).callForCancel(CHALLENGER_ID);
        verify(challengeSpy).callForCancel(ACCEPTOR_ID);
        verify(service).deleteChallenge(any());
    }
}
