package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

import java.util.Optional;

public abstract class ButtonInteractionCommand {

	protected final EloTrackingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;
	protected final ButtonInteractionEvent event;
	protected final Message reporterMessage;
	protected final long guildId;
	protected final Game game;
	protected final ChallengeModel challenge;
	protected final boolean isChallengerCommand;
	protected final long otherPlayerPrivateChannelId;

	protected ButtonInteractionCommand(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot,
									TimedTaskQueue queue, GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.reporterMessage = event.getMessage().get();
		this.client = client;
		Optional<ChallengeModel> maybeChallengeByChallengerMessageId = service.getChallengeByChallengerMessageId(event.getMessageId().asLong());
		if (maybeChallengeByChallengerMessageId.isPresent()) {
			this.challenge = maybeChallengeByChallengerMessageId.get();
			this.guildId = challenge.getGuildId();
			this.isChallengerCommand = true;
		} else {
			this.challenge = service.getChallengeByAcceptorMessageId(event.getMessageId().asLong()).get();
			this.guildId = challenge.getGuildId();
			this.isChallengerCommand = false;
		}
		this.game = service.findGameByGuildId(guildId).get();
		this.otherPlayerPrivateChannelId = Long.parseLong(event.getCustomId().split(":")[1]);
	}

	public abstract void execute();
}
