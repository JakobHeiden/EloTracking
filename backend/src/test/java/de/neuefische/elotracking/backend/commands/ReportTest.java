package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportTest extends CommandTest {

	private final String REPORTER_ID = SnowflakeTestFactory.CHALLENGER_ID;
	private final Snowflake REPORTER = SnowflakeTestFactory.CHALLENGER;
	private final String REPORTED_ON_ID = SnowflakeTestFactory.ACCEPTOR_ID;
	private final Snowflake REPORTED_ON = SnowflakeTestFactory.ACCEPTOR;
	private List<ChallengeModel> challenges = ChallengeModelTestFactory.createList();
	private ChallengeModel challenge = ChallengeModelTestFactory.create();

	@BeforeEach
	void initService() {
		when(service.findGameByChannelId(SnowflakeTestFactory.CHANNEL_ID)).thenReturn(GameTestFactory.create());
	}

	@Test
	@DisplayName("If there is no challenge present no calls to service should be made")
	void noChallenge() {
		when(service.findChallenge(ChallengeModelTestFactory.create().getId())).thenReturn(Optional.empty());
		String text = String.format("!win @%s", REPORTED_ON_ID);
		Message msg = MessageTestFactory.createMock(text, REPORTER);
		command = new Win(msg, service, bot);

		command.execute();

		verify(service, never()).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@Test
	@DisplayName("If the challenge is not accepted no calls to service should be made")
	void challengeNotAccepted() {
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		String text = String.format("!win @%s", REPORTED_ON_ID);
		Message msg = MessageTestFactory.createMock(text, REPORTER);
		command = new Win(msg, service, bot);

		command.execute();

		verify(service, never()).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@Test
	@DisplayName("If the other guy has not reported yet any result should get accepted")
	void firstToChallenge() {
		challenge.accept();
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		String text = String.format("!win @%s", REPORTED_ON_ID);
		Message msg = MessageTestFactory.createMock(text, REPORTER);
		command = new Win(msg, service, bot);

		command.execute();

		verify(service).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@Test
	@DisplayName("If the other guy has already reported a conflicting result, that should be saved but no match should be generated")
	void conflictingReports() {
		challenge.accept();
		challenge.setAcceptorReported(ChallengeModel.ReportStatus.WIN);
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		String text = String.format("!win @%s", REPORTED_ON_ID);
		Message msg = MessageTestFactory.createMock(text, REPORTER);
		command = new Win(msg, service, bot);

		command.execute();

		verify(service).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@Test
	@DisplayName("If the other guy has already reported a consistent result, that should be saved and a match should be generated")
	void consistentReports() {
		challenge.accept();
		challenge.setAcceptorReported(ChallengeModel.ReportStatus.LOSS);
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		when(service.updateRatings(any())).thenReturn(new double[]{1600, 1600, 1600, 1600});
		String text = String.format("!win @%s", REPORTED_ON_ID);
		Message msg = MessageTestFactory.createMock(text, REPORTER);
		command = new Win(msg, service, bot);

		command.execute();

		verify(service).saveChallenge(any());
		verify(service).saveMatch(any());
	}
	//TODO allgemeine faelle
}
