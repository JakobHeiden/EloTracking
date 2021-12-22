package de.neuefische.elotracking.backend.commands;

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
public class Accept extends ButtonInteractionCommand {

	public Accept(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
				  GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		long acceptorId = event.getInteraction().getUser().getId().asLong();
		Message acceptorMessage = event.getMessage().get();
		ChallengeModel challenge = service.getChallengeByAcceptorMessageId(acceptorMessage.getId().asLong()).get();

		service.addNewPlayerIfPlayerNotPresent(guildId, acceptorId);
		challenge.setAccepted(true);
		queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY,
				game.getAcceptedChallengeDecayTime(), challenge.getChallengerMessageId());
		service.saveChallenge(challenge);

		Message challengerMessage = bot.getMessageById(
				otherPlayerPrivateChannelId, challenge.getChallengerMessageId()).block();
		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addLine("They have accepted your challenge.")
				.addLine("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")
				.makeLastLineBold();
		challengerMessage.edit().withContent(challengerMessageContent.get())
				.withComponents(createActionRow(acceptorMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe();

		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.makeAllNotBold()
				.addLine("You have accepted the challenge.")
				.addLine("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")
				.makeLastLineBold();
		acceptorMessage.edit().withContent(acceptorMessageContent.get())
				.withComponents(createActionRow(challengerMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe();

		event.acknowledge().subscribe();
	}

	private static ActionRow createActionRow(long channelId, boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
			Button.primary("win:" + channelId,
					Emojis.arrowUp, "Win"),
			Button.primary("lose:" + channelId,
					Emojis.arrowDown, "Lose"),
			Button.primary("draw:" + channelId,
					Emojis.leftRightArrow, "Draw"),
			Button.danger("cancel:" + channelId,
					Emojis.crossMark, "Cancel match"));
		else return ActionRow.of(
				Button.primary("win:" + channelId,
						Emojis.arrowUp, "Win"),
				Button.primary("lose:" + channelId,
						Emojis.arrowDown, "Lose"),
				Button.danger("cancel:" + channelId,
						Emojis.crossMark, "Cancel match"));
	}
}
