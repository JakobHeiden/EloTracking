package de.neuefische.elotracking.backend.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.neuefische.elotracking.backend.commands.SnowflakeTestFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChallengeTest extends CommandTest {

	@BeforeEach
	void initService() {
		when(service.findGameByChannelId(CHANNEL_ID)).thenReturn(GameTestFactory.create());
	}

	@Test
	void cantChallengeSelf() {
		String text = String.format("!challenge @%s" , CHALLENGER_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Challenge(msg, service, bot);

		command.execute();

		verify(service, never()).addNewChallenge(any(), any());
	}

	@Test
	void cantChallengeTwice() {
		when(service.challengeExistsById(ChallengeModelTestFactory.create().getId())).thenReturn(true);
		String text = String.format("!challenge @%s", ACCEPTOR_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Challenge(msg, service, bot);

		command.execute();

		verify(service, never()).addNewChallenge(any(), any());
	}

	@Test
	void happyPathShouldWork() {
		String text = String.format("!challenge @%s", ACCEPTOR_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Challenge(msg, service, bot);

		command.execute();

		verify(service).addNewChallenge(any(), any());
	}

}
