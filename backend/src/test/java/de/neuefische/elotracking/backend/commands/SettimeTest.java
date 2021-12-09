package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.testfactories.MessageTestFactory;
import de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory.ADMIN;
import static org.mockito.Mockito.*;

// This uses only Setmatchautoresolve as the sister classes are all very similar and very simple
@ExtendWith(MockitoExtension.class)
public class SettimeTest extends CommandTest {

	@BeforeEach
	void init() {
		when(service.getPropertiesLoader().getNumberOfTimeSlots()).thenReturn(9999);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "a", "0", "-3"})
	void improperArguments(String timeToken) {
		msg = MessageTestFactory.createMock("!Setautomatchresolve " + timeToken, SnowflakeTestFactory.ADMIN);
		command = new Setmatchautoresolve(msg, service, bot, queue);

		command.execute();

		verify(service, never()).saveGame(any());
	}

	@Test
	void happyPath() {
		msg = MessageTestFactory.createMock("!Setautomatchresolve 99", ADMIN);
		command = new Setmatchautoresolve(msg, service, bot, queue);

		command.execute();

		verify(service).saveGame(any());
	}
}
