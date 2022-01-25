package com.elorankingbot.backend;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import discord4j.core.GatewayDiscordClient;
import org.junit.jupiter.api.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.awaitility.Awaitility.*;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTests {

	@SpyBean
	private EloRankingService service;
	@Autowired
	private DiscordBotService bot;
	@Autowired
	private GatewayDiscordClient client;
	@Autowired
	private IntegrationTestEventFactory eventFactory;
	private long entenwieseId;
	private long enteId;
	private long ente2Id;

	@BeforeAll
	void clearChallenge() {
		loadProps();
		Optional<ChallengeModel> maybeChallenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id);
		if (maybeChallenge.isPresent()) {
			service.deleteChallengeById(maybeChallenge.get().getId());
			await().until(() -> service.findChallengeByParticipants(
					entenwieseId, enteId, ente2Id).isEmpty());
		}
	}

	@BeforeEach
	void loadProps() {
		this.entenwieseId = service.getPropertiesLoader().getEntenwieseId();
		this.enteId = service.getPropertiesLoader().getOwnerId();
		this.ente2Id = service.getPropertiesLoader().getEnte2Id();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void pathToDecline() {
		client.getEventDispatcher().publish(eventFactory.mockChallengeEvent());

		await().until(() -> service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).isPresent());

		ChallengeModel challenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();
		client.getEventDispatcher().publish(eventFactory
				.mockChallengeButtonEvent(challenge, "decline", false));

		await().until(() -> service.findChallengeById(challenge.getId()).isEmpty());
	}

	@Test
	void pathToWin() {
		client.getEventDispatcher().publish(eventFactory.mockChallengeEvent());

		await().until(() -> service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).isPresent());
		ChallengeModel challenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();

		client.getEventDispatcher().publish(eventFactory
				.mockChallengeButtonEvent(challenge, "accept", false));

		await().until(() -> service.findChallengeById(challenge.getId()).get().isAccepted());
		ChallengeModel acceptedChallenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();

		client.getEventDispatcher().publish(eventFactory
				.mockChallengeButtonEvent(acceptedChallenge, "win", true));

		await().until(() -> service.findChallengeById(acceptedChallenge.getId()).get()
				.getChallengerReported() == ChallengeModel.ReportStatus.WIN);
		ChallengeModel winReportedChallenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();

		client.getEventDispatcher().publish(eventFactory
				.mockChallengeButtonEvent(winReportedChallenge, "lose", false));

		await().until(() -> service.findChallengeById(winReportedChallenge.getId()).isEmpty());
		verify(service).saveMatch(any());
	}

	@Test
	void pathToDraw() {
		client.getEventDispatcher().publish(eventFactory.mockChallengeEvent());

		await().until(() -> service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).isPresent());
		ChallengeModel challenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();

		client.getEventDispatcher().publish(eventFactory.
				mockChallengeButtonEvent(challenge, "accept", false));

		await().until(() -> service.findChallengeById(challenge.getId()).get().isAccepted());
		ChallengeModel acceptedChallenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();

		client.getEventDispatcher().publish(eventFactory
				.mockChallengeButtonEvent(acceptedChallenge, "draw", true));

		await().until(() -> service.findChallengeById(acceptedChallenge.getId()).get()
				.getChallengerReported() == ChallengeModel.ReportStatus.DRAW);
		ChallengeModel winReportedChallenge = service.findChallengeByParticipants(
				entenwieseId, enteId, ente2Id).get();

		client.getEventDispatcher().publish(eventFactory
				.mockChallengeButtonEvent(winReportedChallenge, "draw", false));

		await().until(() -> service.findChallengeById(winReportedChallenge.getId()).isEmpty());
		verify(service).saveMatch(any());
	}
}
