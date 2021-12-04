package de.neuefische.elotracking.backend;

import de.neuefische.elotracking.backend.dao.ChallengeDao;
import de.neuefische.elotracking.backend.dao.GameDao;
import de.neuefische.elotracking.backend.dao.MatchDao;
import de.neuefische.elotracking.backend.dao.PlayerDao;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.model.Player;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.testfactories.ChallengeModelTestFactory;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.neuefische.elotracking.backend.testfactories.SnowflakeTestFactory.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EloTrackingServiceTest {

	@Mock private DiscordBotService bot;
	@Mock private TimedTaskQueue queue;
	@Mock private GameDao gameDao;
	@Mock private ChallengeDao challengeDao;
	@Mock private MatchDao matchDao;
	@Mock private PlayerDao playerDao;
	@InjectMocks
	private EloTrackingService service;

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void testTimedAutoResolveMatch(int caseNumber) {
		ChallengeModel challenge = ChallengeModelTestFactory.create();
		switch (caseNumber) {
			case 1:
				challenge.setChallengerReported(ChallengeModel.ReportStatus.WIN);
				break;
			case 2:
				challenge.setChallengerReported(ChallengeModel.ReportStatus.LOSS);
				break;
			case 3:
				challenge.setAcceptorReported(ChallengeModel.ReportStatus.WIN);
				break;
			case 4:
				challenge.setAcceptorReported(ChallengeModel.ReportStatus.LOSS);
		}
		when(challengeDao.findById(challenge.getId())).thenReturn(Optional.of(challenge));

		String challengerPlayerId = Player.generateId(CHANNEL_ID, CHALLENGER_ID);
		String acceptorPlayerId = Player.generateId(CHANNEL_ID, ACCEPTOR_ID);
		when(playerDao.findById(challengerPlayerId)).thenReturn(Optional.of(new Player(CHANNEL_ID, CHALLENGER_ID, 1600)));
		when(playerDao.findById(acceptorPlayerId)).thenReturn(Optional.of(new Player(CHANNEL_ID, ACCEPTOR_ID, 1600)));

		service.timedAutoResolveMatch(challenge.getId(), 999);

		ArgumentCaptor<Match> match = ArgumentCaptor.forClass(Match.class);
		verify(matchDao).save(match.capture());
		if (caseNumber == 1 || caseNumber == 4) {
			assertEquals(CHALLENGER_ID, match.getValue().getWinner());
			assertEquals(ACCEPTOR_ID, match.getValue().getLoser());
		} else {
			assertEquals(ACCEPTOR_ID, match.getValue().getWinner());
			assertEquals(CHALLENGER_ID, match.getValue().getLoser());
		}
		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(bot).sendToChannel(any(), text.capture());
		System.out.println(text.getValue());
	}
}
