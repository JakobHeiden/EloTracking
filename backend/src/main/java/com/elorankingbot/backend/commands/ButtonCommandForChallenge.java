package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

import java.util.Optional;

public abstract class ButtonCommandForChallenge extends ButtonCommand {

	protected final Message parentMessage;
	protected final Message targetMessage;
	protected final long guildId;
	protected final Game game;
	protected final ChallengeModel challenge;
	protected final boolean isChallengerCommand;

	protected ButtonCommandForChallenge(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
										TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);

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
		this.parentMessage = event.getMessage().get();
		long targetUserPrivateChannelId = Long.parseLong(event.getCustomId().split(":")[1]);
		this.targetMessage = isChallengerCommand ?
				bot.getMessageById(targetUserPrivateChannelId, challenge.getAcceptorMessageId()).block()
				: bot.getMessageById(targetUserPrivateChannelId, challenge.getChallengerMessageId()).block();
	}
}
