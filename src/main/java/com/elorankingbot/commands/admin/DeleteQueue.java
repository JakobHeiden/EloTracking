package com.elorankingbot.commands.admin;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.command.annotations.QueueCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@AdminCommand
@QueueCommand
public class DeleteQueue extends SlashCommand {

	public DeleteQueue(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		List<ApplicationCommandOptionChoiceData> queueChoices = server.getQueues().stream()
				.map(queue -> String.format("%s %s", queue.getGame().getName(), queue.getName()))
				.map(queueFullName -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
						.name(queueFullName).value(queueFullName).build())
				.toList();
		return ApplicationCommandRequest.builder()
				.name(DeleteQueue.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("queue").description("Which queue to delete")
						.type(STRING.getValue())
						.addAllChoices(queueChoices)
						.required(true).build())
				.build();
	}

	public static String getShortDescription() {
		return "Delete a queue from a ranking.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `queue` The queue you want to delete.\n" +
				"For more information on queues, see `/help:` `Concept: Rankings and Queues`.";
	}

	protected void execute() {
		String queueFullName = event.getOption("queue").get().getValue().get().asString();
		Game game = server.getGame(queueFullName.split(" ")[0]);
		game.deleteQueue(queueFullName.split(" ")[1]);
		dbService.saveServer(server);
		String updatedCommands = discordCommandManager.updateQueueCommands(server, exceptionHandler.updateCommandFailedCallbackFactory(event));

		event.reply(String.format("Deleted queue %s. Updated or deleted these commands: %s" +
						"\nThis may take a few minutes to deploy on the server.",
				queueFullName, updatedCommands)).subscribe();
	}
}
