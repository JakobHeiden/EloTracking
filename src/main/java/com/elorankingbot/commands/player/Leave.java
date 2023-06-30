package com.elorankingbot.commands.player;

import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.command.annotations.PlayerCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Player;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

@PlayerCommand
@GlobalCommand
public class Leave extends SlashCommand {

	public Leave(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("leave")
				.description("Leave all matchmaking queues")
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
		queueScheduler.removePlayerFromAllQueues(server, player);
		event.reply("I removed you from all queues you were in, if any.").withEphemeral(true).subscribe();
	}
}
