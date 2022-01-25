package com.elorankingbot.backend.commands.challenge;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

public abstract class ButtonCommandRelatedToChallenge extends ButtonCommand {

	protected final Message parentMessage;
	protected final Message targetMessage;
	protected final long guildId;
	protected final Game game;
	protected final ChallengeModel challenge;
	protected final boolean isChallengerCommand;

	protected ButtonCommandRelatedToChallenge(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
											  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);

		long challengeId = Long.parseLong(event.getCustomId().split(":")[1]);
		this.challenge = service.findChallengeById(challengeId).get();
		this.guildId = challenge.getGuildId();
		if (event.getInteraction().getChannelId().asLong() == challenge.getChallengerChannelId()) {
			this.isChallengerCommand = true;
		} else {
			this.isChallengerCommand = false;
		}
		this.game = service.findGameByGuildId(guildId).get();
		this.parentMessage = event.getMessage().get();
		this.targetMessage = isChallengerCommand ?
				bot.getMessageById(challenge.getAcceptorChannelId(), challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(challenge.getChallengerChannelId(), challenge.getChallengerMessageId()).block();
	}

	protected void updateAndSaveChallenge(Message message) {
		if (isChallengerCommand) updateAcceptorMessageIdsAndSaveChallenge(message);
		else updateChallengerMessageIdsAndSaveChallenge(message);
	}

	private void updateChallengerMessageIdsAndSaveChallenge(Message message) {
		challenge.setChallengerMessageId(message.getId().asLong());
		challenge.setChallengerChannelId(message.getChannelId().asLong());
		service.saveChallenge(challenge);
	}

	private void updateAcceptorMessageIdsAndSaveChallenge(Message message) {
		challenge.setAcceptorMessageId(message.getId().asLong());
		challenge.setAcceptorChannelId(message.getChannelId().asLong());
		service.saveChallenge(challenge);
	}
}
