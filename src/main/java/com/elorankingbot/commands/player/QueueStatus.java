package com.elorankingbot.commands.player;

import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.command.annotations.PlayerCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.commands.admin.AddQueue;
import com.elorankingbot.commands.admin.CreateRanking;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

@PlayerCommand
@GlobalCommand
public class QueueStatus extends SlashCommand {

	public QueueStatus(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(QueueStatus.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.build();
	}

	public static String getShortDescription() {
		return "Display all queues on the server, and the number of players in each of them.";
	}

	public static String getLongDescription() {
		return getShortDescription();
	}

	protected void execute() {
		List<EmbedCreateFields.Field> embedFields = server.getQueues().stream().map(queue -> EmbedCreateFields
				.Field.of(queue.getFullName(), makeNumPlayersString(queue.getNumPlayersWaiting()), true)).toList();
		if (embedFields.isEmpty()) {
			String noQueuesMessage = String.format("No queues on the server. Use `/%s` and `/%s`.",
					CreateRanking.class.getSimpleName().toLowerCase(), AddQueue.class.getSimpleName().toLowerCase());
			embedFields = List.of(EmbedCreateFields.Field.of("No queues on the server", noQueuesMessage, false));
		}
		event.reply().withEmbeds(EmbedCreateSpec.builder()
						.title("Queue Info")
						.addAllFields(embedFields)
						.build())
				.withEphemeral(true).subscribe();
	}

	private static String makeNumPlayersString(int numPlayers) {
		if (numPlayers == 0) return "empty";
		if (numPlayers == 1) return "1 player";
		return String.format("%s players", numPlayers);
	}
}
