package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class Addpermission extends SlashCommand {

	public Addpermission(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
						 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("addpermission")
				.description("Add an elo permission level to an existing role")
				.addOption(ApplicationCommandOptionData.builder()
						.name("admin").description("Add elo admin permission level to an existing role")
						.type(1)
						.addOption(ApplicationCommandOptionData.builder()
								.name("role").description("Add elo admin permission level to this role")
								.type(8).required(true).build())
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("moderator").description("Add elo moderator permission level to an existing role")
						.type(1)
						.addOption(ApplicationCommandOptionData.builder()
								.name("role").description("Add elo moderator permission level to this role")
								.type(8).required(true).build())
						.build())
				.build();
	}

	// TODO! fuer everyone ausfuerhbar machen

	public void execute() {

	}
}
