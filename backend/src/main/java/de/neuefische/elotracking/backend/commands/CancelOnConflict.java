package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;

import java.util.ArrayList;

public class CancelOnConflict extends ButtonCommand {// TODO! klassenname??

	protected CancelOnConflict(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
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
		service.saveChallenge(challenge);

		if (!bothCalledForCancel) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:. If your opponent does as well, the match will be canceled. " +
							"You can still file a dispute.")
					.makeLastLineBold();
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(ActionRow.of(
							Button.secondary("dispute:" + targetMessage.getChannelId().asString(),
									Emojis.exclamation, "File a dispute"))).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:.");
			targetMessage.edit().withContent(targetMessageContent.get()).subscribe();
			return;
		}

		if (bothCalledForCancel) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a cancel :negative_squared_cross_mark:. The match is canceled.");
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(new ArrayList<>()).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. The match is canceled.");
			targetMessage.edit().withContent(targetMessageContent.get()).subscribe();

			service.deleteChallenge(challenge);
		}


	}
}
