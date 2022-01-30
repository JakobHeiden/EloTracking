package com.elorankingbot.backend.commands.challenge;

import com.elorankingbot.backend.commands.ButtonCommandRelatedToChallengeOrDispute;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

public abstract class ButtonCommandRelatedToChallenge extends ButtonCommandRelatedToChallengeOrDispute {

	protected final Message parentMessage;
	protected final Message targetMessage;
	protected final boolean isChallengerCommand;

	protected ButtonCommandRelatedToChallenge(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
											  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		if (event.getInteraction().getChannelId().asLong() == challenge.getChallengerChannelId()) {
			this.isChallengerCommand = true;
		} else {
			this.isChallengerCommand = false;
		}
		this.parentMessage = event.getMessage().get();
		this.targetMessage = isChallengerCommand ?
				bot.getMessageById(challenge.getAcceptorChannelId(), challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(challenge.getChallengerChannelId(), challenge.getChallengerMessageId()).block();
	}

	protected void updateAndSaveChallenge(Message message) {// TODO vllt in interface, default method refaktorn
		if (isChallengerCommand) updateAcceptorMessageIdAndSaveChallenge(message);
		else updateChallengerMessageIdAndSaveChallenge(message);
	}
}
