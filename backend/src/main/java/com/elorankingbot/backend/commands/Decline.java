package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Decline extends ButtonCommandForChallenge {

	public Decline(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot,
				   TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		Message acceptorMessage = event.getMessage().get();
		ChallengeModel challenge = service.getChallengeByAcceptorMessageId(acceptorMessage.getId().asLong()).get();

		service.deleteChallengeById(challenge.getChallengerMessageId());

		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.makeAllNotBold()
				.addLine("You have declined :negative_squared_cross_mark: their challenge.")
				.makeAllItalic();
		acceptorMessage.edit().withContent(acceptorMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		Message challengerMessage = bot.getMessageById(
				Long.parseLong(event.getCustomId().split(":")[1]), challenge.getChallengerMessageId()).block();
		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addLine("They have declined :negative_squared_cross_mark: your challenge.")
				.makeAllItalic();
		challengerMessage.edit().withContent(challengerMessageContent.get()).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), null);
	}
}
