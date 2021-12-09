package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.testfactories.GameTestFactory;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory.CHANNEL_ID;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class CommandTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	protected EloTrackingService service;
	@Mock protected DiscordBotService bot;
	@Mock protected TimedTaskQueue queue;
	protected Message msg;
	protected Command command;

	@BeforeEach
	void initGame() {
		when(service.findGameByChannelId(CHANNEL_ID)).thenReturn(GameTestFactory.create());
	}

	@AfterEach
	void printBotReplies() {
		command.getBotReplies().forEach(System.out::println);
	}

}
