package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.testfactories.ChallengeModelTestFactory;
import de.neuefische.elotracking.backend.testfactories.MessageTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChallengeTest extends CommandTest {

	@Test
	void cantChallengeSelf() {
		String text = String.format("!challenge @%s" , CHALLENGER_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Challenge(msg, service, bot, queue);

		command.execute();

		verify(service, never()).saveChallenge(any());
		verify(queue, never()).addTimedTask(any(), anyInt(), any());
	}

	@Test
	void cantChallengeTwice() {
		when(service.challengeExistsById(ChallengeModelTestFactory.create().getId())).thenReturn(true);
		String text = String.format("!challenge @%s", ACCEPTOR_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Challenge(msg, service, bot, queue);

		command.execute();

		verify(queue, never()).addTimedTask(any(), anyInt(), any());
		verify(service, never()).saveChallenge(any());
	}

	@Test
	void happyPathShouldWork() {
		String text = String.format("!challenge @%s", ACCEPTOR_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Challenge(msg, service, bot, queue);

		command.execute();

		verify(queue).addTimedTask(any(), anyInt(), any());
		verify(service).saveChallenge(any());
	}

}
