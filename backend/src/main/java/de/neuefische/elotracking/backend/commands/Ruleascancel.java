package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Ruleascancel extends ButtonCommandForDispute {

	public Ruleascancel(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		service.deleteChallenge(challenge);

		postToDisputeChannel(String.format(
				"%s has ruled the challenge to be canceled. <@%s> <@%s>",
				moderatorName, challenge.getChallengerId(), challenge.getAcceptorId()));
		postToChallengerAndAcceptorChannels();
		addMessageDeleteToQueue();
		event.acknowledge().subscribe();
	}

	private void postToChallengerAndAcceptorChannels() {
		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addLine(String.format("%s has ruled this to be canceled :negative_squared_cross_mark:.", moderatorName))
				.makeAllItalic();
		challengerMessage.edit().withContent(challengerMessageContent.get())
				.withComponents(none).subscribe();

		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.addLine(String.format("%s has ruled this to be canceled :negative_squared_cross_mark:.", moderatorName))
				.makeAllItalic();
		acceptorMessage.edit().withContent(acceptorMessageContent.get())
				.withComponents(none).subscribe();
	}

	private void addMessageDeleteToQueue() {
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), null);
	}
}
