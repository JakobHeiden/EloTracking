package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Cancelonconflict extends ButtonCommand {

	public Cancelonconflict(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		boolean bothCalledForCancel = false;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForCancel(true);
			bothCalledForCancel = challenge.isAcceptorCalledForCancel();
		} else {
			challenge.setAcceptorCalledForCancel(true);
			bothCalledForCancel = challenge.isChallengerCalledForCancel();
		}

		if (!bothCalledForCancel) oneCalledForCancel(parentMessage, targetMessage, challenge, service);
		if (bothCalledForCancel) bothCalledForCancel(parentMessage, targetMessage, challenge, game, service, queue);
		event.acknowledge().subscribe();
	}

	static void oneCalledForCancel(Message parentMessage, Message targetMessage, ChallengeModel challenge,
								   EloTrackingService service) {
		service.saveChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You called for a cancel :negative_squared_cross_mark:. If your opponent does as well, " +
						"the match will be canceled. You can still file a dispute.")
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. " +
						"You can agree to a cancel or file a dispute.");
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.agreeToCancelOnConflict(parentMessage.getChannelId().asLong()),
						Buttons.dispute(parentMessage.getChannelId().asLong())
				)).subscribe();
	}

	static void bothCalledForCancel(Message parentMessage, Message targetMessage,
									ChallengeModel challenge, Game game,
									EloTrackingService service, TimedTaskQueue queue) {
		service.deleteChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You agreed to a cancel :negative_squared_cross_mark:. The match is canceled.");
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.makeAllNotBold()
				.addLine("Your opponent agreed to a cancel :negative_squared_cross_mark:. The match is canceled.");
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.DELETE_MESSAGE, game.getMessageCleanupTime(),
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.DELETE_MESSAGE, game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), null);
	}
}
