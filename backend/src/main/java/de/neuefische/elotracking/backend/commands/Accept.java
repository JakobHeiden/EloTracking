package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Accept extends ButtonCommand {

	public Accept(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
				  GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		long parentId = event.getInteraction().getUser().getId().asLong();
		Message parentMessage = event.getMessage().get();
		ChallengeModel challenge = service.getChallengeByAcceptorMessageId(parentMessage.getId().asLong()).get();

		service.addNewPlayerIfPlayerNotPresent(guildId, parentId);
		challenge.setAccepted(true);
		queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY,
				game.getAcceptedChallengeDecayTime(), challenge.getChallengerMessageId(), 0L, null);
		service.saveChallenge(challenge);

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("They have accepted your challenge.")
				.addLine(String.format("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:%s",
						game.isAllowDraw() ? " or drew :left_right_arrow:" : ""))
				.makeLastLineBold();
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(createActionRow(parentMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe();

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You have accepted the challenge.")
				.addLine(String.format("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:",
						game.isAllowDraw() ? " or drew :left_right_arrow:" : ""))
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(createActionRow(targetMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe();

		event.acknowledge().subscribe();
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
