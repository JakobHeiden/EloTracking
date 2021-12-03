package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.testfactories.ChallengeModelTestFactory;
import de.neuefische.elotracking.backend.testfactories.GameTestFactory;
import de.neuefische.elotracking.backend.testfactories.MessageTestFactory;
import de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

import static de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory.*;

// logic for WIN vs LOSS is simple enough and omitted
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportTest extends CommandTest {

	private static Stream<ChallengeModel> challengeModelStream() {
		return Stream.of(ChallengeModelTestFactory.create(), ChallengeModelTestFactory.createButReverseChallengerAndAcceptor());
	}

	@BeforeEach
	void initService() {
		when(service.findGameByChannelId(SnowflakeTestFactory.CHANNEL_ID)).thenReturn(GameTestFactory.create());
	}

	@Test
	@DisplayName("If there is no challenge present no calls to service should be made")
	void noChallenge() {
		when(service.findChallenge(ChallengeModelTestFactory.create().getId())).thenReturn(Optional.empty());
		String text = String.format("!win @%s", ACCEPTOR_ID);
		Message msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Win(msg, service, bot, queue);

		command.execute();

		verify(service, never()).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@ParameterizedTest
	@MethodSource("challengeModelStream")
	@DisplayName("If the challenge is not accepted no calls to service should be made")
	void challengeNotAccepted(ChallengeModel challenge) {
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		String text = String.format("!win @%s", ACCEPTOR_ID);
		Message msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Win(msg, service, bot, queue);

		command.execute();

		verify(service, never()).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@ParameterizedTest
	@MethodSource("challengeModelStream")
	@DisplayName("If the other guy has not reported yet any result should get accepted")
	void firstToChallenge(ChallengeModel challenge) {
		challenge.accept();
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		String text = String.format("!win @%s", ACCEPTOR_ID);
		Message msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Win(msg, service, bot, queue);

		command.execute();

		verify(service).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@ParameterizedTest
	@MethodSource("challengeModelStream")
	@DisplayName("If the other guy has already reported a conflicting result, that should be saved but no match should be generated")
	void conflictingReports(ChallengeModel challenge) {
		challenge.accept();
		challenge.setAcceptorReported(ChallengeModel.ReportStatus.WIN);
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		String text = String.format("!win @%s", ACCEPTOR_ID);
		Message msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Win(msg, service, bot, queue);

		command.execute();

		verify(service).saveChallenge(any());
		verify(service, never()).saveMatch(any());
	}

	@ParameterizedTest
	@MethodSource("challengeModelStream")
	@DisplayName("If the other guy has already reported a consistent result, that should be saved and a match should be generated")
	void consistentReports(ChallengeModel challenge) {
		challenge.accept();
		if (challenge.getChallengerId().equals(CHALLENGER_ID)) {
			challenge.setAcceptorReported(ChallengeModel.ReportStatus.LOSS);
		} else if (challenge.getChallengerId().equals(ACCEPTOR_ID)) {
			challenge.setChallengerReported(ChallengeModel.ReportStatus.LOSS);
		}
		when(service.findChallenge(challenge.getId())).thenReturn(Optional.of(challenge));
		when(service.updateRatings(any())).thenReturn(new double[]{1600, 1600, 1600, 1600});
		String text = String.format("!win @%s", ACCEPTOR_ID);
		Message msg = MessageTestFactory.createMock(text, CHALLENGER);
		command = new Win(msg, service, bot, queue);

		command.execute();

		verify(service).saveChallenge(any());
		verify(service).saveMatch(any());
	}
}
