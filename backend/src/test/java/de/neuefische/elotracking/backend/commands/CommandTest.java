package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;

public abstract class CommandTest {

	@Mock protected EloTrackingService service;
	@Mock protected DiscordBotService bot;
	protected Message msg;
	protected Command command;

	@AfterEach
	void printBotReplies() {
		command.getBotReplies().forEach(System.out::println);
	}

}
