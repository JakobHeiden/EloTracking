package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.List;
import java.util.Optional;

@Slf4j
public class Accept extends ButtonInteractionCommand {

	public Accept(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
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

		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.makeAllNotBold()
				.addNewLine("You have accepted the challenge.")
				.addNewLine("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")
				.makeLastLineBold();
		acceptorMessage.edit().withContent(acceptorMessageContent.get())
				.withComponents(ActionRow.of(
						Button.primary("win:" + acceptorMessage.getChannelId().asString(),
								Emojis.arrowUp, "Win"),
						Button.primary("lose:" + acceptorMessage.getChannelId().asString(),
								Emojis.arrowDown, "Lose"),
						Button.primary("draw:" + acceptorMessage.getChannelId().asString(),
								Emojis.leftRightArrow, "Draw"),
						Button.danger("cancel:" + acceptorMessage.getChannelId().asString(),
								Emojis.crossMark, "Cancel match")))
				.subscribe();

		Message challengerMessage = bot.getMessageById(
				Long.parseLong(event.getCustomId().split(":")[1]), challenge.getChallengerMessageId()).block();
		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addNewLine("They have accepted your challenge.")
				.addNewLine("Come back after the match and let me know if you won :arrow_up: or lost :arrow_down:")
				.makeLastLineBold();
		challengerMessage.edit().withContent(challengerMessageContent.get())
				.withComponents(ActionRow.of(
						Button.primary("win:" + challengerMessage.getChannelId().asString(),
								Emojis.arrowUp, "Win"),
						Button.primary("lose:" + challengerMessage.getChannelId().asString(),
								Emojis.arrowDown, "Lose"),
						Button.primary("draw:" + challengerMessage.getChannelId().asString(),
								Emojis.leftRightArrow, "Draw"),
						Button.danger("cancel:" + challengerMessage.getChannelId().asString(),
								Emojis.crossMark, "Cancel match")))
				.subscribe();

		event.acknowledge().subscribe();
	}
}
