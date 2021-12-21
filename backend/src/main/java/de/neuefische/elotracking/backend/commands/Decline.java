package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;

public class Decline extends ButtonInteractionCommand {

	public Decline(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
	}

	public void execute() {
		Message acceptorMessage = event.getMessage().get();
		ChallengeModel challenge = service.getChallengeByAcceptorMessageId(acceptorMessage.getId().asLong()).get();

		service.deleteChallengeById(challenge.getChallengerMessageId());

		MessageContent acceptorMessageContent = new MessageContent(acceptorMessage.getContent())
				.makeAllNotBold()
				.addNewLine("You have declined :negative_squared_cross_mark: their challenge.")
				.makeAllItalic();
		acceptorMessage.edit().withContent(acceptorMessageContent.get())
				.withComponents(new ArrayList<>()).subscribe();

		Message challengerMessage = bot.getMessageById(
				Long.parseLong(event.getCustomId().split(":")[1]), challenge.getChallengerMessageId()).block();
		MessageContent challengerMessageContent = new MessageContent(challengerMessage.getContent())
				.addNewLine("They have declined :negative_squared_cross_mark: your challenge.")
				.makeAllItalic();
		challengerMessage.edit().withContent(challengerMessageContent.get()).subscribe();

		event.acknowledge().subscribe();
	}
}
