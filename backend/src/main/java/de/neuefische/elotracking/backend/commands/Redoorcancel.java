package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

public class Redoorcancel extends ButtonCommand {

	public Redoorcancel(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		boolean bothCalledForRedo = false;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForRedo(true);
			bothCalledForRedo = challenge.isAcceptorCalledForRedo();
		} else {
			challenge.setAcceptorCalledForRedo(true);
			bothCalledForRedo = challenge.isChallengerCalledForRedo();
		}

		boolean bothCalledForCancel = false;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForCancel(true);
			bothCalledForCancel = challenge.isAcceptorCalledForCancel();
		} else {
			challenge.setAcceptorCalledForCancel(true);
			bothCalledForCancel = challenge.isChallengerCalledForCancel();
		}

		boolean opponentDidNotCallEither = !bothCalledForCancel && !bothCalledForRedo;

		if (opponentDidNotCallEither) opponentDidNotCallEither();
		else if (bothCalledForRedo) Redo.bothCalledForRedo(parentMessage, targetMessage, challenge, game, service);
		else if (bothCalledForCancel) Cancelonconflict.bothCalledForCancel(parentMessage, targetMessage,
				challenge, game, service, queue);
	}

	private void opponentDidNotCallEither() {
		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You called for a redo or a cancel :person_shrugging:. You can still file a dispute.")
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine("Your opponent called for a redo or a cancel :person_shrugging:.");
		targetMessage.edit().withContent(targetMessageContent.get()).subscribe();
	}
}

