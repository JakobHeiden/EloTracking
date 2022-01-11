package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.Buttons;
import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Accept extends ButtonCommandForChallenge {

	public Accept(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
				  GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		Message parentMessage = event.getMessage().get();
		ChallengeModel challenge = service.getChallengeByAcceptorMessageId(parentMessage.getId().asLong()).get();

		challenge.setAccepted(true);
		queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY,
				game.getAcceptedChallengeDecayTime(), challenge.getChallengerMessageId(), 0L, null);
		service.saveChallenge(challenge);

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("They have accepted your challenge.")
				.addLine(String.format("Come back after the match and let me know if you won or lost %s.",
						game.isAllowDraw() ? " or drew" : ""))
				.makeLastLineBold();
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(createActionRow(parentMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe();

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You have accepted the challenge.")
				.addLine(String.format("Come back after the match and let me know if you won or lost %s.",
						game.isAllowDraw() ? " or drew" : ""))
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(createActionRow(targetMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe();

		event.deferEdit().subscribe();
	}

	private static ActionRow createActionRow(long channelId, boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.draw(channelId),
				Buttons.cancel(channelId));
		else return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.cancel(channelId));
	}
}