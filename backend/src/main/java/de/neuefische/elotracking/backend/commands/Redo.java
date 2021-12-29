package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Buttons;
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
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Redo extends ButtonCommand {

	public Redo(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				TimedTaskQueue queue, GatewayDiscordClient client) {
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

		if (!bothCalledForRedo) oneCalledForRedo();
		if (bothCalledForRedo) bothCalledForRedo(parentMessage, targetMessage, challenge, game, service);
		event.acknowledge().subscribe();
	}

	private void oneCalledForRedo() {
		service.saveChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine("You called for a redo :leftwards_arrow_with_hook:. If your opponent does as well, " +
						"reports will be redone. You can still file a dispute.");
		parentMessage.edit().withContent(parentMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.makeAllNotBold()
				.makeLastLineStrikeThrough()
				.addLine("Your opponent called for a redo :leftwards_arrow_with_hook:. " +
						"You can agree to a redo or file a dispute.")
				.makeLastLineBold();
		targetMessage.edit().withContent(targetMessageContent.get())
				.withComponents(ActionRow.of(
						Buttons.agreeToRedo(parentMessage.getChannelId().asLong()),
						Buttons.dispute(parentMessage.getChannelId().asLong()))).subscribe();
	}

	static void bothCalledForRedo(Message parentMessage, Message targetMessage, ChallengeModel challenge, Game game,
								  EloTrackingService service) {
		challenge.redo();
		service.saveChallenge(challenge);

		MessageContent parentMessageContent = new MessageContent(parentMessage.getContent())
				.makeAllNotBold()
				.addLine(String.format("You agreed to a redo :leftwards_arrow_with_hook:. Reports are redone. " +
						"Did you win or lose%s", game.isAllowDraw() ? " or draw?" : "?"))
				.makeLastLineBold();
		parentMessage.edit().withContent(parentMessageContent.get()).withComponents(
				createActionRow(targetMessage.getChannelId().asLong(), game.isAllowDraw())).subscribe();

		MessageContent targetMessageContent = new MessageContent(targetMessage.getContent())
				.addLine(String.format("Your opponent agreed to a redo :leftwards_arrow_with_hook:. Reports are redone. " +
						"Did you win or lose%s", game.isAllowDraw() ? " or draw?" : "?"))
				.makeLastLineBold();
		targetMessage.edit().withContent(targetMessageContent.get()).withComponents(
				createActionRow(parentMessage.getChannelId().asLong(), game.isAllowDraw())).subscribe();
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
