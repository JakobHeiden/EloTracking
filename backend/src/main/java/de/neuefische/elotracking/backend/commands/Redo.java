package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

import java.util.ArrayList;

public class Redo extends ButtonCommand {

	public Redo(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
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
		service.saveChallenge(challenge);

		if (!bothCalledForRedo) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine("You called for a redo :leftwards_arrow_with_hook:. If your opponent does as well, " +
							"reports will be redone. You can still file a dispute.")
					.makeLastLineBold();
			parentMessage.edit().withContent(parentMessageContent.get())
					.withComponents(ActionRow.of(
							Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine("Your opponent called for a redo :leftwards_arrow_with_hook:.");
			targetMessage.edit().withContent(targetMessageContent.get()).subscribe();
			return;
		}

		if (bothCalledForRedo) {
			MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
					.makeAllNotBold()
					.addLine(String.format("You called for a redo :leftwards_arrow_with_hook:. Reports are redone. " +
									"Did you win or lose%s", game.isAllowDraw() ? " or draw?" : "?"));
			parentMessage.edit().withContent(parentMessageContent.get()).withComponents(
							createActionRow(targetMessage.getChannelId().asLong(), game.isAllowDraw())).subscribe();

			MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
					.addLine(String.format("Your opponent called for a redo :leftwards_arrow_with_hook:. Reports are redone. " +
									"Did you win or lose%s", game.isAllowDraw() ? " or draw?" : "?"));
			targetMessage.edit().withContent(targetMessageContent.get()).withComponents(
							createActionRow(parentMessage.getChannelId().asLong(), game.isAllowDraw())).subscribe();

			challenge.redo();
			service.saveChallenge(challenge);
		}
	}

	private static ActionRow createActionRow(long channelId, boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.draw(channelId));
		else return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId));
	}
}
