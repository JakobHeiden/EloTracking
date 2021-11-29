package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static de.neuefische.elotracking.backend.commands.SnowflakeTestFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChallengeTest {

	@Mock private EloTrackingService service;
	@Mock private DiscordBotService bot;
	private Message msg;
	private Command challengeCommand;
	private List<ChallengeModel> challenges = ChallengeModelTestFactory.createList();

	@BeforeEach
	void initService() {
		when(service.findGameByChannelId(CHANNEL_ID)).thenReturn(GameTestFactory.create());
	}

	@Test
	void cantChallengeSelf() {
		String text = String.format("!challenge @%s" , CHALLENGER_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		challengeCommand = new Challenge(msg, service, bot);

		challengeCommand.execute();

		verify(service, never()).addChallenge(any(), any());
	}

	@Test
	void cantChallengeTwice() {
		when(service.challengeExistsById(ChallengeModelTestFactory.create().getId())).thenReturn(true);
		String text = String.format("!challenge @%s", ACCEPTOR_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		challengeCommand = new Challenge(msg, service, bot);

		challengeCommand.execute();

		verify(service, never()).addChallenge(any(), any());
	}

	@Test
	void happyPathShouldWork() {
		String text = String.format("!challenge @%s", ACCEPTOR_ID);
		msg = MessageTestFactory.createMock(text, CHALLENGER);
		challengeCommand = new Challenge(msg, service, bot);

		challengeCommand.execute();

		verify(service).addChallenge(any(), any());
	}
}
