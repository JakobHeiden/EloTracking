package com.elorankingbot.backend.commands.player.challenge;

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
	protected final boolean isChallengerCommand;
	protected final ChallengeModel challenge;
	protected final long guildId;
	protected final Game game;

	protected ButtonCommandRelatedToChallenge(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
											  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.challenge = service.findChallengeById(Long.parseLong(event.getCustomId().split(":")[1])).get();
		this.guildId = challenge.getGuildId();
		this.game = service.findGameByGuildId(guildId).get();
		if (event.getInteraction().getChannelId().asLong() == challenge.getChallengerChannelId()) {
			this.isChallengerCommand = true;
		} else {
			this.isChallengerCommand = false;
		}
		this.parentMessage = event.getMessage().get();
		this.targetMessage = isChallengerCommand ?
				bot.getMessageById(challenge.getAcceptorMessageId(), challenge.getAcceptorChannelId()).block()
				: bot.getMessageById(challenge.getChallengerMessageId(), challenge.getChallengerChannelId()).block();
	}

	protected void updateAndSaveChallenge(Message message) {// TODO vllt in interface, default method refaktorn
		if (isChallengerCommand) updateAcceptorMessageIdAndSaveChallenge(message);
		else updateChallengerMessageIdAndSaveChallenge(message);
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
