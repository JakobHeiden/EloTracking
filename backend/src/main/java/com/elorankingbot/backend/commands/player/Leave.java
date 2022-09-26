package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.command.annotations.PlayerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

@PlayerCommand
public class Leave extends SlashCommand {

	public Leave(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("leave")
				.description("Leave all matchmaking queues")// TODO option fuer nur eine leaven
				.defaultPermission(true)
				.build();
	}

	public static String getShortDescription() {
		return "Leave all queues you are currently in.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
		"For more information on queues, see `/help`:`Concept: Rankings and Queues`.";
	}

	protected void execute() {
		Player player = dbService.getPlayerOrGenerateIfNotPresent(guildId, activeUser);
		queueService.removePlayerFromAllQueues(server, player);
		event.reply("I removed you from all queues you were in, if any.").withEphemeral(true).subscribe();
		// TODO auflisten, welche queues verlassen wurden
	}
}
