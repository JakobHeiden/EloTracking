package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

public abstract class ButtonCommandRelatedToChallengeOrDispute extends ButtonCommand {

	protected final ChallengeModel challenge;
	protected final long guildId;
	protected final Game game;

	protected ButtonCommandRelatedToChallengeOrDispute(ButtonInteractionEvent event, EloRankingService service,
													   DiscordBotService bot, TimedTaskQueue queue,
													   GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.challenge = service.findChallengeById(Long.parseLong(event.getCustomId().split(":")[1])).get();
		this.guildId = challenge.getGuildId();
		this.game = service.findGameByGuildId(guildId).get();
	}

	protected void updateChallengerMessageIdAndSaveChallenge(Message message) {
		challenge.setChallengerMessageId(message.getId().asLong());
		service.saveChallenge(challenge);
	}

	protected void updateAcceptorMessageIdAndSaveChallenge(Message message) {
		challenge.setAcceptorMessageId(message.getId().asLong());
		service.saveChallenge(challenge);
	}
}
