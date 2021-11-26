package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ChallengeTest {

	@Mock private EloTrackingService service;
	@Mock private DiscordBotService bot;
	private Message msg;
	private Command challengeCommand;
	private List<ChallengeModel> challenges = ChallengeModelTestFactory.createList();

	//TODO
	//can"t challenge self
	//can't challenge twice
	//challenge does appear



}
